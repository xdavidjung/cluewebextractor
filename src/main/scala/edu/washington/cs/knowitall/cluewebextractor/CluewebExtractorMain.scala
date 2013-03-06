package edu.washington.cs.knowitall.cluewebextractor

import scala.io.Source

import de.l3s.boilerpipe.extractors
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer

// CLI that takes a filename as an argument and outputs the extracted clueweb
// information. 
// takes a second argument for the profiles needed by the language detection
// module.
object CluewebExtractorMain extends App {
  // check for the proper number of command line arguments
  usage(args.length)

  // get the warc record input and create the iterator
  val source = getSource(args)
  val warcIt = new WarcRecordIterator(source.getLines)

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
  } println(warc.warcTrecId + "\t" +
            i + "\t" +
            sentence)

  source.close()

  def usage(numArgs: Int) {
    if (numArgs != 1 && numArgs != 2) {
      System.err.println("Usage1: gunzip -c <input.warc.gz> | java -jar " +
                         "<this.jar> <profiles/>\nUsage2: java -jar " +
                         "<this.jar> <profiles/> <input.warc>")
      System.exit(1)
    }
  }

  def getSource(args: Array[String]): Source = {
    if (args.length == 2) {
      // then the user has passed in a filename: use it
      Source.fromFile(args(1), "ISO-8859-1")
    } else {
      // then the user will pass in the file through stdin
      Source.fromInputStream(System.in, "ISO-8859-1")
    }
  }
}
