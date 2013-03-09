package edu.washington.cs.knowitall.cluewebextractor
import org.jsoup.Jsoup;
import scala.util.matching.Regex;

// this class contains a number of methods that detect garbage in sentences
// taken from the web.
class GarbageFilter(dir: String) {

  // removes series of extraneous whitespace in the input sentence
  def removeWhitespace(input: String): String = {
    new Regex("""\s+""").replaceAllIn(input, " ")
  }

  // returns true if input contains HTML content
  def containsHtml(input: String): Boolean = {
    input.startsWith("HTTP/1.1") ||  // is an HTTP request
    input.contains("</") ||  // closing tags
    input.contains("/>") ||  // self-closing tags
    input.contains("<a href=") ||  // anchor tags
    input.matches(".*<.{1,4}>.*")  // opening tags, 1-4 chars
  }

  // returns true if input contains less than six words (detected by the
  // number of spaces in input)
  def tooShort(input: String): Boolean = {
    // split on whitespace and count what's left
    val split = input.split("\\s+");

    split.length < 6
  }

  // returns true if the input is too long: more than 300 chars.
  def tooLong(input: String): Boolean = {
    input.length > 500
  }
}
