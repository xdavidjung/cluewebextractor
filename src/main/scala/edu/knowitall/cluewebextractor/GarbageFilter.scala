package edu.knowitall.cluewebextractor
import org.jsoup.Jsoup;
import scala.util.matching.Regex;

// this class contains a number of methods that detect garbage in sentences
// taken from the web.
class GarbageFilter() {

  // Removes series of extraneous whitespace in the input sentence
  // On error simply returns the input
  def removeWhitespace(input: String): String = {
    try {
      new Regex("""\s+""").replaceAllIn(input, " ")
    } catch {
      case e: Throwable =>
        input
    }
  }

  // Returns true if input contains HTML content, false otherwise.
  def containsHtml(input: String): Boolean = {
    input.startsWith("HTTP/1.1") ||  // is an HTTP request
      input.contains("</") ||        // closing tags
      input.contains("/>") ||        // self-closing tags
      input.contains("<a href=") ||  // anchor tags
      input.matches(".*<.{1,4}>.*")  // opening tags, 1-4 chars
  }

  // Returns true if input contains less than six words (detected by the
  // number of spaces in input)
  def tooShort(input: String): Boolean = {
    // iterate through and find spaces
    var numSpaces = 0
    for (c <- input; if c == ' ') {
      numSpaces += 1
      if (numSpaces >= 5) {
        return false
      }
    }
    true
  }

  // returns true if the input is too long: more than 300 chars.
  def tooLong(input: String): Boolean = {
    input.length > 500
  }
}
