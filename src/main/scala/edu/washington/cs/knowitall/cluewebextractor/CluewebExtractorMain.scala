package edu.washington.cs.knowitall.cluewebextractor

import de.l3s.boilerpipe.extractors;
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer;

// CLI that takes a filename as an argument and outputs the extracted clueweb
// information. 
// takes a second argument for the profiles needed by the language detection
// module.
object CluewebExtractorMain extends App {
  // check for the proper number of command line arguments
  if (args.length != 2) usage

  val garbager = new GarbageFilter(args(1))
  val bp = extractors.ArticleSentencesExtractor.getInstance
  val sentencer = new OpenNlpSentencer("en-sent.bin")

  // get an iterator over the lines of the file specified
  val warcIt = new WarcRecordIterator(args(0))

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

  warcIt.close()

  def usage() {
    System.err.println("Usage: java -jar <program> warc-filename " +
                       "profiles-directory")
    System.exit(1)
  }
}
