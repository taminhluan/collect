package org.gfd.collect.caferedd.utilities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.gfd.collect.caferedd.upload.InstanceGoogleSheetsUploader;

public class InstanceGoogleSheetsUploaderTaskTest {
    @Test
    public void gpsLocationRegexTests() {
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("{}{"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("28"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("-@123"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid(";'[@123"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("*&1w345"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("41 24.2028, 2 10.4418"));
        assertFalse(InstanceGoogleSheetsUploader.isLocationValid("41.40338"));
        assertTrue(InstanceGoogleSheetsUploader.isLocationValid("-9.9 -9.9 -9.9 9.9"));
        assertTrue(InstanceGoogleSheetsUploader.isLocationValid("-0.0 0.8 -9.7 9.9"));
        assertTrue(InstanceGoogleSheetsUploader.isLocationValid("8.0 0.8 8.7 8.9"));
    }
}