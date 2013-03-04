package edu.washington.cs.knowitall.cluewebextractor
import scala.io.Source;

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
  val sentencer = new OpenNlpSentencer()

  // get an iterator over the lines of the file specified
  val warcIt = new WarcRecordIterator(Source.fromFile(args(0), "iso-8859-1").getLines)

  while(warcIt.hasNext) {
    val warc = warcIt.next()

    if (warc != null) {
      // pass through boilerpipe
      val piped = bp.getText(warc.payload)

      // check if the payload is english
      if (garbager.onlyLatinChars(piped) && garbager.isEnglish(piped)) {

        // split into sentences
        val sentences = sentencer.sentences(piped)

        // for each sentence, pass through garbager and then print out the:
        // id, uri, sentence number, sentence
        // separated by tabs
        var i = 0;
        for (s <- sentences) {
          val sentence = garbager.removeWhitespace(s)
          if (!garbager.tooShort(sentence) &&
              !garbager.containsHtml(sentence) &&
              !garbager.tooLong(sentence)) {
            println(warc.warcTrecId + "\t" +
                    warc.warcTargetUri + "\t" +
                    i.toString + "\t" +
                    sentence)
          }
          i = i + 1;
        }
      }
    }
  }

  def usage() {
    System.err.println("Usage: java -jar <program> warc-filename profiles-directory");
    System.exit(1);
  }
}
