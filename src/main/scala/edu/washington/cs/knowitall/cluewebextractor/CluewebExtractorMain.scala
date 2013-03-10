package edu.washington.cs.knowitall.cluewebextractor

import de.l3s.boilerpipe.extractors
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer
import edu.washington.cs.knowitall.common.Timing
import edu.washington.cs.knowitall.common.Resource
import scala.collection.JavaConverters._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileWriter
import java.io.PrintStream
import java.io.InputStream
import java.io.FileInputStream
import java.io.File
import java.io.PrintWriter
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

// CLI that takes a filename as an argument and outputs the extracted clueweb
// information.
// takes a second argument for the profiles needed by the language detection
// module.
object CluewebExtractorMain extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  case class Config(
    inputFiles: Seq[File] = Seq.empty,
    outputDirectory: Option[File] = None) {
    def outputFile(inputFile: File): File = {
      val name = inputFile.getName().takeWhile(_ != '.') + ".sentences"
      outputDirectory match {
        case Some(dir) => new File(dir, name)
        case None => new File(name)
      }
    }
  }

  val parser = new scopt.immutable.OptionParser[Config]("cweb") {
    def options = Seq(
      arglist("<input-files>", "pattern file") { (path: String, config: Config) =>
        val file = new File(path)
        require(file.exists(), "file does not exist: " + path)
        config.copy(inputFiles = (config.inputFiles :+ file))
      },
      opt("output-dir", "output directory") { (path: String, config: Config) =>
        val file = new File(path)
        require(file.exists, "directory does not exist: " + path)
        config.copy(outputDirectory = Some(file))
      })
  }

  parser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None => System.err.println(usage)
  }

  def run(config: Config) {
    // get the warc record input and create the iterator
    for (file <- config.inputFiles) {
      val ns = Timing.time {
        Resource.using(openInputStream(file)) { is =>
          Resource.using(new PrintWriter(config.outputFile(file), "UTF8")) {
            writer =>
              val warcIt = new WarcRecordIterator(
                             new DataInputStream(
                             new BufferedInputStream(is))
                           )
            logger.info("Successfully created new warc iterator")

            val garbager = new GarbageFilter()

            val nlpSentencer = new OpenNlpSentencer("en-sent.bin")
            val bp = new extractors.DefaultExtractor()

            var lastDocument = 0
            var nanos = System.nanoTime()
            for {
              // iterate over warc responses
              warc <- warcIt.flatten
              if warc.warcType.equals("response")
            } {
              val piped = bp.getText(warc.payload.trim)
              val sentences = nlpSentencer.segmentTexts(piped)

              // iterate over sentences
              var i = 0
              for {
                s <- sentences

                // apply garbage filter
                sentence = garbager.removeWhitespace(s)
                if !garbager.containsHtml(sentence);
                if !garbager.tooLong(sentence);
                if !garbager.tooShort(sentence)
              } {
                if (warcIt.currentDocument % 100 == 0 &&
                    lastDocument != warcIt.currentDocument) {
                  logger.info("Processing: " + warcIt.currentDocument + " (" +
                              ("%.2f" format (warcIt.currentDocument.toDouble /
                              ((System.nanoTime - nanos).toDouble /
                              Timing.Seconds.divisor.toDouble))) + " doc/sec)")
                  lastDocument = warcIt.currentDocument
                }

                writer.println(warc.warcTrecId + "\t" +
                               warc.warcUri + "\t" +
                               warc.warcDate + "\t" +
                               i + "\t" +
                               sentence)
                i += 1

              }
            }
          }
        }
      }

      logger.info("Processed file '" + file.getName + "' in: " +
                  Timing.Seconds.format(ns))
    }
  }

  def usage {
    "Usage: java -jar <this.jar> <input.warc(.gz)>"
  }

  def openInputStream(file: File): InputStream = {
    if (file.getName endsWith ".gz") {
      // then the user has passed in .warc.gz file
      logger.info("Opening zip file " + file)
      new GZIPInputStream(new FileInputStream(file))
    } else {
      // then the user has passed in .warc file
      logger.info("Opening file " + file)
      new FileInputStream(file)
    }
  }
}
