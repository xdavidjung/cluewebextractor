package edu.washington.cs.knowitall.cluewebextractor
import org.jsoup.Jsoup;

// this class contains a number of methods that detect garbage in sentences
// taken from the web.
class GarbageFilter {

  // returns whether all the characters in input are within an acceptable
  // range of characters (common english chars).
  def onlyLatinChars(input: String): Boolean = {
    for (char <- input) {
      // safe chars: 0 to 126, 160 to 255, 8211 to 8230, 8482 
      if (((char.toInt > 126 && char.toInt < 160) ||
          (char.toInt > 255 && char.toInt < 8211)) &&
          (char.toInt != 8482)) {
        return false;
      }
    }
    return true;
  }

  // returns true if input contains HTML content
  def containsHtml(input: String): Boolean = {
    if (input.contains("</")) {
      return true;  // closing tags
    }
    if (input.contains("/>")) {
      return true; // self-closing tags
    }
    if (input.contains("<a href=")) {
      return true;  // anchor tags
    }
    if (input.matches(".*<.{1,4}>.*")) {
      return true;  // opening tags, 1-4 chars
    }
    return false;
  }

  // returns true if input contains less than three words (detected by the
  // number of spaces in input)
  def tooShort(input: String): Boolean = {
    // split on whitespace and count what's left
    val split = input.split("\\s+");

    if (split.length < 3) return true;
    return false;
  }
}
