package de.holarse.rssgrabber.feedhandler;

import de.holarse.rssgrabber.output.OutputUtils;
import de.holarse.rssgrabber.output.TitleAdjustOption;
import static org.junit.Assert.*;
import org.junit.Test;

public class OutputUtilsTest {

    @Test
    public void testKeepStartingWhitespaceWhenColonFound() {
        final String input = " Mein Spiel: Der erste Teil";
        assertEquals("should keep whitespaces when colon", " Mein Spiel: Der erste Teil", OutputUtils.sanatize(input));
    }    
    
    @Test
    public void testRemoveStartingWhitespaceIfWanted() {
        final String input = " Mein Spiel: Der erste Teil";
        assertEquals("should remove trailing whitespace if wanted", "Mein Spiel: Der erste Teil", OutputUtils.sanatize(input, TitleAdjustOption.TRIM));
    }     
    
    @Test
    public void testRemoveEmojis1() {
        final String input = "Instinct™ MI200 Memory Space Overview – amd-lab-notes.";
        assertEquals("should remove emojis as drupal dont likes them", "Instinct MI200 Memory Space Overview  amd-lab-notes.", OutputUtils.sanatize(input, TitleAdjustOption.TRIM));
    }    
    
    @Test
    public void testRemoveEmojis1a() {
        final String input = "Instinct™ MI200 Memory Space Overview - amd-lab-notes.";
        assertEquals("should remove emojis as drupal dont likes them", "Instinct MI200 Memory Space Overview - amd-lab-notes.", OutputUtils.sanatize(input, TitleAdjustOption.TRIM));
    }       
    
    @Test
    public void testRemoveEmojis2() {
        final String input = "Dev Diary #104: AI AI AI! 🤖";
        assertEquals("should remove emojis as drupal dont likes them", "Dev Diary #104: AI AI AI!", OutputUtils.sanatize(input, TitleAdjustOption.TRIM));
    }    

    @Test
    public void testRemoveNewlines() {
        final String input = "This is my title\nwith a newline";
        assertEquals("should have no newline", "This is my title with a newline", OutputUtils.sanatize(input));
    }    
    
    @Test
    public void testRemoveWindowsNewlines() {
        final String input = "This is my title\r\nwith a newline";
        assertEquals("should have no newline", "This is my title with a newline", OutputUtils.sanatize(input));
    }        
    
}
