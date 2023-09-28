package de.holarse.rssgrabber.feedhandler;

import static org.junit.Assert.*;
import org.junit.Test;

public class GithubReleasesFeedHandlerTest {

    @Test
    public void testGithubUrlBuilderFromAtomFeedUrl() {
        final String input = "https://github.com/AntiMicroX/antimicrox/releases.atom";
        assertEquals("should build from atom-releases-path", "https://api.github.com/repos/AntiMicroX/antimicrox/releases?page=1&per_page=20", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }

    @Test
    public void testGithubUrlBuilderFromProjectPage() {
        final String input = "https://github.com/AntiMicroX/antimicrox/";
        assertEquals("should build from project page", "https://api.github.com/repos/AntiMicroX/antimicrox/releases?page=1&per_page=20", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }    
    
    @Test
    public void testGithubUrlBuilderFromOwnernRepo() {
        final String input = "AntiMicroX/antimicrox";
        assertEquals("should build from owner/repo", "https://api.github.com/repos/AntiMicroX/antimicrox/releases?page=1&per_page=20", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }     
    
    @Test
    public void testUrlOpenNox() {
        final String input = "https://github.com/noxworld-dev/opennox/releases";
        assertEquals("should build correct url", "https://api.github.com/repos/noxworld-dev/opennox/releases?page=1&per_page=20", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }
    
    @Test
    public void testTagsUrlInsteadOfReleases() {
        final String input = "https://github.com/noxworld-dev/opennox/tags";
        assertEquals("should build correct url", "https://github.com/noxworld-dev/opennox/tags.atom", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }    
    
    @Test
    public void testTagsUrlInsteadOfReleasesDotAtom() {
        final String input = "https://github.com/noxworld-dev/opennox/tags.atom";
        assertEquals("should build correct url", "https://github.com/noxworld-dev/opennox/tags.atom", GitHubReleasesFeedHandler.buildGithubUrl(input, GitHubReleasesFeedHandler.getGithubFeedMode(input)));
    }     
    
}
