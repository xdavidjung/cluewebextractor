package edu.washington.cs.knowitall.cluewebextractor

import scala.collection.mutable

// This class provides a way to iterate over the WARC records in a ClueWeb12
// .warc file. This means that it is assumed that the format of the WARC
// records will match up with those described at:
//   http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
// @author David H Jung
class WarcRecordIterator(fileIt: Iterator[String]) extends Iterator[WarcRecord] {
  // The number of documents in this warc file
  private var numberOfDocuments: Int = -1

  // The current document. Used for debugging.
  private var currentDocument: Int = -1

  // The latest line that fileIt.next has returned.
  private var current: String = null

  // Whether this iterator is valid.
  private var valid = true

  initialize()

  // Returns true if there is another warc entry in file.
  // Can mutate the state of fileIt: if hasNext returns true,
  // then fileIt will be at the beginning of the next warcEntry.
  def hasNext(): Boolean = {
    if (!valid) {
      valid
    }

    // check if fileIt is already at the next entry
    if (current.equals(WarcRecordIterator.recordStarter)) {
      return valid
    }

    // a warc file has a next warcEntry if we can find a line in fileIt that
    // is equal to recordStarter
    while (fileIt.hasNext) {
      nextLine()
      if (current.equals(WarcRecordIterator.recordStarter)) {
        return valid
      }
    }

    // hasNext returned false: no more warc entries,
    // this iterator is no longer valid
    valid = false
    valid
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

    currentDocument = currentDocument + 1
    new WarcRecord(warcType, warcTrecId, sb.toString)
  }

  // Opens the file given in the constructor and slurps up the warcinfo header
  // for this file, setting fileIt to the beginning of the first warc record.
  private def initialize(): Unit = {
    // just a quick check that this is indeed a warc file:
    require(nextLine().equals(WarcRecordIterator.recordStarter),
            "Input does not follow WARC 1.0 format")
    require(nextLine().split(": ")(1).equals(WarcRecordIterator.HeaderType),
            "Input does not follow WARC 1.0 format")

    // now process the header: the goal is to get the number of docs and set
    // the file iterator at the beginning of the first actual warc record
    while(fileIt.hasNext) {
      nextLine()
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
