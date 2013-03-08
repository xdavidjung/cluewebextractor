package edu.washington.cs.knowitall.cluewebextractor

import scala.collection.mutable
import scalax.io._
import scalax.io.JavaConverters._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// This class provides a way to iterate over the WARC records in a ClueWeb12
// .warc file. This means that it is assumed that the format of the WARC
// records will match up with those described at:
//   http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf
// @author David H Jung
class WarcRecordIterator(fileBytes: LongTraversable[Byte]) extends Iterator[Option[WarcRecord]] {
  // The number of documents in this warc file
  private var numberOfDocuments: Int = -1

  // The current document. Used for debugging.
  private var currentDocument: Int = -1

  // The latest line that has been read from fileBytes.
  private var current: String = null

  // Whether this iterator is valid.
  private var valid = true

  private val logger = LoggerFactory.getLogger(this.getClass)

  initialize()

  // Returns true if there is another warc entry in file.
  // Can mutate the state of fileBytes: if hasNext returns true,
  // then fileBytes will be at the beginning of the next warcEntry.
  def hasNext(): Boolean = {
    if (!valid) {
      valid
    }

    // a warc file has a next warcEntry if we can find a line in fileBytes that
    // is equal to recordStarter
    while (!current.equals(WarcRecordIterator.recordStarter)) {
      nextLine()
    }

    if (!current.equals(WarcRecordIterator.recordStarter)) {
      valid = false
    }

    valid
  }

  // Returns the next WarcRecord in this file.
  // throws NoSuchElementException if there is no next WarcRecord.
  def next(): Option[WarcRecord] = {
    if (!hasNext) {
      throw new NoSuchElementException()
    }

    logger.info("Getting next WARC record. Current document: " +
                currentDocument)

    // Grab lines and process fields until we hit the end of block indicator
    val warcFieldMap = mutable.Map.empty[String, String]
    nextLine()
    while (!current.equals(WarcRecordIterator.endOfBlock)) {
      val splitLine = current.split(": ")

      // all lines until the end of the block should be splittable
      if (splitLine.length != 2) {
        logger.error("Bad WARC header: no ': ' sequence in document: " +
                     currentDocument + " on line: " + current)
        return None
      }

      warcFieldMap(splitLine(0)) = splitLine(1)
      nextLine()
    }

    // Get the fields we want
    val warcType = warcFieldMap(WarcRecordIterator.typeIndicator).dropRight(1)
    val warcTrecId = warcFieldMap(WarcRecordIterator.trecIdIndicator).dropRight(1)
    val warcDate = warcFieldMap(WarcRecordIterator.dateIndicator).dropRight(1)
    val warcUri = warcFieldMap(WarcRecordIterator.uriIndicator).dropRight(1)
    var contentLength = -1

    try {
      contentLength = warcFieldMap(WarcRecordIterator.
                                   contentLengthIndicator).dropRight(1).toInt
    } catch {
      case e: NumberFormatException =>
        logger.error("Unable to convert content length to int with string: " +
                     warcFieldMap(WarcRecordIterator.contentLengthIndicator) +
                     " in document: " + currentDocument)
        return None
    }

    // Get the payload
    val warcPayload = fileBytes.take(contentLength)
                               .asInput
                               .chars(Codec("UTF-8"))
                               .mkString("")

    currentDocument = currentDocument + 1
    new Some(WarcRecord(warcType, warcTrecId, warcDate, warcUri, warcPayload))
  }

  // Gets the next line of input and stores it in current.
  private def nextLine(): String = {
    current = fileBytes.takeWhile(_!='\n')
                       .map(_.toChar)
                       .mkString("")
    current
  }

  // Opens the file given in the constructor and slurps up the warcinfo header
  // for this file, setting fileBytes to the beginning of the first warc record.
  private def initialize(): Unit = {
    // just a quick check that this is indeed a warc file:
    if (!nextLine().equals(WarcRecordIterator.recordStarter)) {
      logger.error("Input first line is not " +
                   WarcRecordIterator.recordStarter)
      valid = false
    }
    if (!nextLine().split(": ")(1).equals(WarcRecordIterator.headerType)) {
      logger.error("Input second line is not " +
                   WarcRecordIterator.headerType)
      valid = false
    }

    // now process the header: the goal is to get the number of docs and set
    // the file iterator at the beginning of the first actual warc record
    currentDocument = 0
    nextLine()
    while(!current.equals(WarcRecordIterator.recordStarter)) {
      if (current.split(": ")(0).equals(WarcRecordIterator.numDocField)) {
        numberOfDocuments = current.split(": ")(1).toInt
      }
      nextLine()
    }
  }
}

object WarcRecordIterator {
  // When found alone, indicates the end of a block (header, payload).
  val endOfBlock = "\r"

  // When found alone, indicates the start of a new WARC record.
  val recordStarter = "WARC/1.0\r"

  // The type of the header WARC record, present at the beginning of
  // all .warc files.
  val headerType = "warcinfo\r"

  // Field in the warcinfo header for the number of documents in this warc file
  val numDocField = "WARC-Number-Of-Documents"

  // indicates the field that gives an id
  val trecIdIndicator = "WARC-TREC-ID"

  // indicates the field that gives a date
  val dateIndicator = "WARC-Date"

  // indicates the field that gives a uri
  val uriIndicator = "WARC-Target-URI"

  // indicates the field that gives the warc type of the record
  val typeIndicator = "WARC-Type"

  // indicates the field that gives content length
  val contentLengthIndicator = "Content-Length"
}
