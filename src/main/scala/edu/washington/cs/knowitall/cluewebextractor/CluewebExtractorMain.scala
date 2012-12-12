package edu.washington.cs.knowitall.cluewebextractor
import scala.io.Source;

import de.l3s.boilerpipe.extractors;
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer;

// takes a filename as a command-line argument and outputs a TODO
object CluewebExtractorMain extends App {
  val bp = extractors.ArticleSentencesExtractor.getInstance
  val sentencer = new OpenNlpSentencer()

  // check for the proper number of command line arguments
  if (args.length != 1) usage();

  // get an iterator over the lines of the file specified
  val warcIt = new WarcEntryIterator(Source.fromFile(args(0), "iso-8859-1").getLines);

  while(warcIt.hasNext) {
    val warc = warcIt.next()
    if (warc != null) {
      // pass through boilerpipe and sentencer
      val piped = bp.getText(warc.payload)
      val sentences = sentencer.sentences(piped)

      // for each sentence, print out the:
      // id, uri, sentence number, sentence
      // separated by tabs
      var i = 0;
      for (sentence <- sentences) {
        println(warc.warcTrecId + "\t" +
                warc.warcTargetUri + "\t" +
                i.toString + "\t" +
                sentence)
        i = i + 1;
      }
    }
  }

  def usage() {
    System.err.println("Usage: java -jar <program> warc-filename");
    System.exit(1);
  }
}
