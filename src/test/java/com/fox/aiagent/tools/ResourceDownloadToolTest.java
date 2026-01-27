package com.fox.aiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png";
        String fileName = "GitHub-Mark.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}