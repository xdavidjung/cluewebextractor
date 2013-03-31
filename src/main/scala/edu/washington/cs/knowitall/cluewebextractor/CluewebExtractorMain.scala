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

/**
 * CLI that takes as input either a .warc file or directory containing .warc
 * files and outputs the extracted payload content in either a default or
 * specified directory.
 *
 * If the user inputs a directory that contains a .warc file with an already-
 * existing corresponding output file, it will be skipped.
 *
 * If the user inputs a single .warc file with an already-existing
 * corresponding output file, it will be overwritten.
 */
object CluewebExtractorMain extends App {
  val logger = LoggerFactory.getLogger(this.getClass)

  case class Config(
    inputFiles: Seq[File] = Seq.empty,
    outputDirectory: Option[File] = None) {
  }

  // Defines the command line arguments.
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
        require(file.isDirectory, "file is not a directory: " + path)
        config.copy(outputDirectory = Some(file))
      })
  }

  parser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None => System.err.println(usage)
  }

  def run(config: Config) {

    // Output filename is the input filename up to and including the first dot
    // with "sentences" as the extension.
    def makeOutputFileName(inputFile: File) = {
      inputFile.getName().takeWhile(_ != '.') + ".sentences"
    }

    // Files contains (inputFile, outputFile) pairs.
    val files: Iterable[(File, File)] = config.inputFiles.flatMap { file =>
      import org.apache.commons.io.FileUtils
      import scala.collection.JavaConverters._

      // if it's a directory, search subdirectories
      if (file.isDirectory) {
        val files: Iterable[File] =
          FileUtils.listFiles(file, Array("gz"), true).asScala

        files.flatMap { inputFile =>
          val subdirectory = inputFile.getParentFile.getPath.drop(file.getParentFile.getPath.length).drop(1)

          // build the output file
          val outputDirectory = config.outputDirectory match {
            case Some(dir) => new File(dir, subdirectory)
            case None => new File(subdirectory)
          }

          // create the file's parent directory if it doesn't exist
          outputDirectory.mkdirs

          val outputFileName = makeOutputFileName(inputFile)
          val outputFile = new File(outputDirectory, outputFileName)

          // if the output file already exists, skip by returning None
          if (outputFile.exists) {
            None
          } else {
            Some(inputFile, outputFile)
          }
        }

      } else {
        // the user input a simple .warc file
        val outputFileName = makeOutputFileName(file)
        val outputFile = config.outputDirectory match {
          case Some(dir) => new File(dir, outputFileName)
          case None => new File(outputFileName)
        }
        Some(file, outputFile)
      }
    }

    // Create the warc record processors
    val garbager = new GarbageFilter()
    val nlpSentencer = new OpenNlpSentencer("en-sent.bin")
    val bp = new extractors.DefaultExtractor()

    // For each (input, output) pair, get a warc record iterator for the input
    // and write the corresponding extracted payload to the output
    for ((inputFile, outputFile) <- files) {
      val ns = Timing.time {
      Resource.using(openInputStream(inputFile)) { is =>
      Resource.using(new PrintWriter(outputFile, "UTF8")) { writer =>

        val warcIt = new WarcRecordIterator(
                       new DataInputStream(
                       new BufferedInputStream(is))
                     )
        logger.info("Successfully created new warc iterator")

        var lastDocument = 0
        var nanos = System.nanoTime()
        for {
          // Iterate over warc responses
          warc <- warcIt.flatten
          if warc.warcType.equals("response")
        } {
          if (warcIt.currentDocument % 1000 == 0 &&
              lastDocument != warcIt.currentDocument) {
            logger.info("Processing document: " + warcIt.currentDocument +
                        " (" +
                        ("%.2f" format (warcIt.currentDocument.toDouble /
                        ((System.nanoTime - nanos).toDouble /
                        Timing.Seconds.divisor.toDouble))) + " doc/sec)")
            lastDocument = warcIt.currentDocument
          }

          val piped = try {
            bp.getText(warc.payload.trim)
          } catch {
            case e: Exception =>
              logger.error("Boilerpipe exception: \n" + e)
              ""
            case _ =>
              logger.error("Boilerpipe error")
              ""
          }

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
            writer.println(warc.warcTrecId + "\t" +
                           warc.warcUri + "\t" +
                           warc.warcDate + "\t" +
                           i + "\t" +
                           sentence)
            i += 1

          }
        }
      }}}

      logger.info("Processed file '" + inputFile.getName + "' -> '"
          + outputFile.getName + "' in: " + Timing.Seconds.format(ns))
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
