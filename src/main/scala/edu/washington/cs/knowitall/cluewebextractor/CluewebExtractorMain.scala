package edu.washington.cs.knowitall.cluewebextractor
import scala.io.Source;

import de.l3s.boilerpipe;

// takes a filename as a command-line argument and outputs a TODO
object CluewebExtractorMain extends App {
  // check for the proper number of command line arguments
  if (args.length != 1) usage();

  // get an iterator over the lines of the file specified
  val warcIt = new WarcEntryIterator(Source.fromFile(args(0)).getLines);

  while(warcIt.hasNext) {
    // TODO write out the warc entry in an extractable format
    val warc = warcIt.next()
    if (warc != null) println(warc.warcTrecId + " length: " + warc.contentLength)
  }

  def usage() {
    System.err.println("Usage: java -jar <program> warc-filename");
    System.exit(1);
  }
}
