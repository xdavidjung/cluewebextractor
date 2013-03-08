package edu.washington.cs.knowitall.cluewebextractor

import java.io.FileInputStream
import java.io.File
import java.io.PrintStream

import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer

import de.l3s.boilerpipe.extractors

import scala.io.Source
import scalax.io.JavaConverters._
import scalax.io._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// CLI that takes a filename as an argument and outputs the extracted clueweb
// information. 
// takes a second argument for the profiles needed by the language detection
// module.
object CluewebExtractorMain extends App {
  // check for the proper number of command line arguments
  usage(args.length)

  val logger = LoggerFactory.getLogger(this.getClass)

  // get the warc record input and create the iterator
  val input = getInput(args)
  val warcIt = new WarcRecordIterator(input.bytes)
  logger.info("Successfully created new warc iterator")

  val outstream = new PrintStream(System.out, true, "UTF-8")
  val garbager = new GarbageFilter(args(0))
  val bp = extractors.ArticleSentencesExtractor.getInstance
  val sentencer = new OpenNlpSentencer("en-sent.bin")

  for {
    warc <- warcIt
    if warc.warcType.equals("response")
    piped = bp.getText(warc.payload)
    if garbager.onlyLatinChars(piped);
    if garbager.isEnglish(piped)
    sentences = sentencer.segmentTexts(piped)
    i <- 0 until sentences.length
    sentence = garbager.removeWhitespace(sentences(i))
    if !garbager.tooShort(sentence);
    if !garbager.containsHtml(sentence);
    if !garbager.tooLong(sentence)
  } outstream.println(warc.warcTrecId + "\t" +
                      warc.warcUri + "\t" +
                      warc.warcDate + "\t" +
                      i + "\t" +
                      sentence)

  // TODO close the file

  def usage(numArgs: Int) {
    if (numArgs != 1 && numArgs != 2) {
      System.err.println("Usage1: gunzip -c <input.warc.gz> | java -jar " +
                         "<this.jar> <profiles/>\nUsage2: java -jar " +
                         "<this.jar> <profiles/> <input.warc>")
      System.exit(1)
    }
  }

  def getInput(args: Array[String]): Input = {
    if (args.length == 2) {
      // then the user has passed in a filename
      logger.info("Opening file " + args(1))
      new FileInputStream(new File(args(1))).asUnmanagedInput
    } else {
      // then the user will pass in the file through stdin
      logger.info("Accepting input from STDIN")
      System.in.asUnmanagedInput
    }
  }
}
