package edu.washington.cs.knowitall.cluewebextractor

import scala.collection.mutable
import scala.io.Source
import scala.util.{Try, Success, Failure}

// This class provides a way to iterate over the WARC records in a ClueWeb12
// .warc file. This means that it is assumed that the format of the WARC
// records will match up with those described at:
//   http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
// @author David H Jung
class WarcRecordIterator(fileName: String) extends Iterator[WarcRecord] {
  // The number of documents in this warc file
  private var numberOfDocuments: Int = -1

  // The current document. Used for debugging.
  private var currentDocument: Int = -1

  // The latest line that fileIt.next has returned.
  private var current: String = null

  // A file iterator for the warc file.
  private var fileIt: Iterator[String] = null

  // The source file of fileIt
  private var source: scala.io.Source = null

  // Whether this iterator is valid.
  private var valid = true

  initialize()

  // Returns true if there is another warc entry in file.
  // Can mutate the state of fileIt: if hasNext returns true,
  // then fileIt will be at the beginning of the next warcEntry.
  def hasNext(): Boolean = {
    if (!valid) {
      return false
    }

    // check if fileIt is already at the next entry
    if (current.equals(WarcRecordIterator.recordStarter)) {
      return true
    }

    // a warc file has a next warcEntry if we can find a line in fileIt that
    // is equal to recordStarter
    while (fileIt.hasNext) {
      nextLine()
      if (current.equals(WarcRecordIterator.recordStarter)) {
        return true
      }
    }

    // hasNext returned false: no more warc entries,
    // this iterator is no longer valid
    valid = false
    return false
  }

  // Returns the next WarcRecord in this file.
  // throws NoSuchElementException if there is no next WarcRecord.
  def next(): WarcRecord = {
    if (!hasNext) {
      throw new NoSuchElementException()
    }

    // Grab lines and process fields until we hit the end of block indicator
    val warcFieldMap = mutable.Map.empty[String, String]
    nextLine()
    while (!current.equals(WarcRecordIterator.endOfBlock)) {
      val splitLine = current.split(": ")
      warcFieldMap(splitLine(0)) = splitLine(1)
      nextLine()
    }

    // Get the fields we want
    val warcType = warcFieldMap(WarcRecordIterator.typeIndicator)
    val warcTrecId = warcFieldMap(WarcRecordIterator.trecIdIndicator)
    val contentLength = warcFieldMap(WarcRecordIterator.
                                     contentLengthIndicator).toInt

    // Get the payload: we know that we're finished with the payload when we:
    //   Hit the end of the file, or 
    //   Are at the beginning of a new record

    // note that contentLength is not the exact length of the input that we
    // store, but it is a good upper bound.
    val sb = new StringBuilder(contentLength)
    nextLine()
    while (fileIt.hasNext &&
           !current.equals(WarcRecordIterator.recordStarter)) {
      sb.append(current)
      sb.append(" ")  // need a space between lines
      nextLine()
    }

    /*
    do {  // grab lines until the line between HTTP header and content
      nextLine()
    } while (current.length > 0)
    */

    currentDocument = currentDocument + 1
    new WarcRecord(warcType, warcTrecId, sb.toString)
  }

  // Closes the file
  def close() = {
    source.close()
  }

  // Opens the file given in the constructor and slurps up the warcinfo header
  // for this file, setting fileIt to the beginning of the first warc record.
  private def initialize(): Unit = {
    // open the file
    val sourceTry = Try(Source.fromFile(fileName, "ISO-8859-1"))
    sourceTry match {
      case Success(s) =>
        source = s
      case Failure(e) =>
        System.err.println("Unable to open file " + fileName)
        System.exit(1)
    }

    fileIt = source.getLines

    // just a quick check that this is indeed a warc file:
    require(nextLine().equals(WarcRecordIterator.recordStarter),
            fileName + " does not seem to be a valid WARC 1.0 file\n" +
            "First line: " + current)
    require(nextLine().split(": ")(1).equals(WarcRecordIterator.HeaderType),
            fileName + "does not seem to be a valid WARC 1.0 file\n" +
            "Second line: " + current)

    // now process the header: the goal is to get the number of docs and set
    // the file iterator at the beginning of the first actual warc record
    while(fileIt.hasNext) {
      nextLine()
      // TODO Add check for number of documents
      if (current.split(": ")(0).equals(WarcRecordIterator.numDocField)) {
        numberOfDocuments = current.split(": ")(1).toInt
      }
      if (current.equals(WarcRecordIterator.recordStarter)) {
        currentDocument = 0
        return
      }
    }
  }

  // Gets the next line of input and stores it in current.
  private def nextLine(): String = {
    if (!fileIt.hasNext) {
      throw new NoSuchElementException()
    }
    current = fileIt.next()
    current
  }
}

object WarcRecordIterator {
  // When found alone, indicates the end of a block (header, payload).
  val endOfBlock = ""

  // When found alone, indicates the start of a new WARC record.
  val recordStarter = "WARC/1.0"

  // The type of the header WARC record, present at the beginning of
  // all .warc files.
  val HeaderType = "warcinfo"

  // Field in the warcinfo header for the number of documents in this warc file
  val numDocField = "WARC-Number-Of-Documents"

  // Field in all ClueWeb records that gives an id
  val trecIdIndicator = "WARC-TREC-ID"

  // indicates the field that gives the warc type of the record
  val typeIndicator = "WARC-Type"

  // indicates the field that gives content length
  val contentLengthIndicator = "Content-Length"
}
