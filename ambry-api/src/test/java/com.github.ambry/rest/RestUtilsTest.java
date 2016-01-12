package com.github.ambry.rest;

import com.github.ambry.messageformat.BlobProperties;
import com.github.ambry.utils.Utils;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Unit tests for {@link RestUtils}.
 */
public class RestUtilsTest {
  private static final Random RANDOM = new Random();
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

  /**
   * Tests building of {@link BlobProperties} given good input (all headers in the number and format expected).
   * @throws Exception
   */
  @Test
  public void getBlobPropertiesGoodInputTest()
      throws Exception {
    JSONObject headers = new JSONObject();
    setAmbryHeaders(headers, Long.toString(RANDOM.nextInt(10000)), Long.toString(RANDOM.nextInt(10000)),
        Boolean.toString(RANDOM.nextBoolean()), generateRandomString(10), "image/gif", generateRandomString(10));
    verifyBlobPropertiesConstructionSuccess(headers);
  }

  /**
   * Tests building of {@link BlobProperties} given varied input. Some input fail (tests check the correct error code),
   * some should succeed (check for default values expected).
   * @throws Exception
   */
  @Test
  public void getBlobPropertiesVariedInputTest()
      throws Exception {
    String contentLength = Long.toString(RANDOM.nextInt(10000));
    String ttl = Long.toString(RANDOM.nextInt(10000));
    String isPrivate = Boolean.toString(RANDOM.nextBoolean());
    String serviceId = generateRandomString(10);
    String contentType = "image/gif";
    String ownerId = generateRandomString(10);

    JSONObject headers;
    // failure required.
    // content length missing.
    headers = new JSONObject();
    setAmbryHeaders(headers, null, ttl, isPrivate, serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.MissingArgs);
    // content length null.
    headers = new JSONObject();
    setAmbryHeaders(headers, null, ttl, isPrivate, serviceId, contentType, ownerId);
    headers.put(RestUtils.Headers.Blob_Size, JSONObject.NULL);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // content length not a number.
    headers = new JSONObject();
    setAmbryHeaders(headers, "NaN", ttl, isPrivate, serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // content length < 0.
    headers = new JSONObject();
    setAmbryHeaders(headers, "-1", ttl, isPrivate, serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // ttl not a number.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, "NaN", isPrivate, serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // ttl < -1.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, "-2", isPrivate, serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // isPrivate not true or false.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, "!(true||false)", serviceId, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // serviceId missing.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, null, contentType, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.MissingArgs);
    // serviceId null.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, null, contentType, ownerId);
    headers.put(RestUtils.Headers.Service_Id, JSONObject.NULL);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // contentType missing.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, serviceId, null, ownerId);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.MissingArgs);
    // contentType null.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, serviceId, null, ownerId);
    headers.put(RestUtils.Headers.Content_Type, JSONObject.NULL);
    verifyBlobPropertiesConstructionFailure(headers, RestServiceErrorCode.InvalidArgs);
    // too many values for all headers.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, serviceId, contentType, ownerId);
    tooManyValuesTest(headers, RestUtils.Headers.Blob_Size);
    tooManyValuesTest(headers, RestUtils.Headers.TTL);
    tooManyValuesTest(headers, RestUtils.Headers.Private);
    tooManyValuesTest(headers, RestUtils.Headers.Service_Id);
    tooManyValuesTest(headers, RestUtils.Headers.Content_Type);
    tooManyValuesTest(headers, RestUtils.Headers.Owner_Id);

    // no failures.
    // ttl missing. Should be infinite time by default.
    // isPrivate missing. Should be false by default.
    // ownerId missing.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, null, null, serviceId, contentType, null);
    verifyBlobPropertiesConstructionSuccess(headers);

    // ttl null.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, null, isPrivate, serviceId, contentType, ownerId);
    headers.put(RestUtils.Headers.TTL, JSONObject.NULL);
    verifyBlobPropertiesConstructionSuccess(headers);

    // isPrivate null.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, serviceId, contentType, ownerId);
    headers.put(RestUtils.Headers.Private, JSONObject.NULL);
    verifyBlobPropertiesConstructionSuccess(headers);

    // ownerId null.
    headers = new JSONObject();
    setAmbryHeaders(headers, contentLength, ttl, isPrivate, serviceId, contentType, null);
    headers.put(RestUtils.Headers.Owner_Id, JSONObject.NULL);
    verifyBlobPropertiesConstructionSuccess(headers);
  }

  /**
   * Tests building of user metadata.
   * @throws Exception
   */
  @Test
  public void getUserMetadataTest()
      throws Exception {
    byte[] usermetadata = RestUtils.buildUsermetadata(createRestRequest(RestMethod.POST, "/", null));
    assertArrayEquals("Unexpected user metadata", new byte[0], usermetadata);
  }

  /**
   * Tests building of User Metadata with good input
   * @throws Exception
   */
  @Test
  public void getUserMetadataGoodInputTest()
      throws Exception {
    JSONObject headers = new JSONObject();
    setAmbryHeaders(headers, Long.toString(RANDOM.nextInt(10000)), Long.toString(RANDOM.nextInt(10000)),
        Boolean.toString(RANDOM.nextBoolean()), generateRandomString(10), "image/gif", generateRandomString(10));
    Map<String, List<String>> userMetadata = new HashMap<String, List<String>>();
    userMetadata.put(RestUtils.Headers.UserMetaData_Header_Prefix + "key1",
        new ArrayList<String>(Arrays.asList("value1_1", "value1_2")));
    userMetadata.put(RestUtils.Headers.UserMetaData_Header_Prefix + "key2",
        new ArrayList<String>(Arrays.asList("value2_1", "value2_2")));
    RestUtils.setAmbryHeaders(headers, userMetadata);
    verifyBlobPropertiesConstructionSuccess(headers);
    verifyUserMetadataConstructionSuccess(headers, userMetadata);
  }

  /**
   * Tests building of User Metadata with unusual input
   * @throws Exception
   */
  @Test
  public void getUserMetadataUnusualInputTest()
      throws Exception {
    JSONObject headers = new JSONObject();
    setAmbryHeaders(headers, Long.toString(RANDOM.nextInt(10000)), Long.toString(RANDOM.nextInt(10000)),
        Boolean.toString(RANDOM.nextBoolean()), generateRandomString(10), "image/gif", generateRandomString(10));
    Map<String, List<String>> userMetadata = new HashMap<String, List<String>>();
    userMetadata.put(RestUtils.Headers.UserMetaData_Header_Prefix + "key1",
        new ArrayList<String>(Arrays.asList("value1_1", "value1_2")));
    userMetadata.put("key2", new ArrayList<String>(Arrays.asList("value2_1", "value2_2"))); // no valid prefix
    userMetadata.put("key3" + RestUtils.Headers.UserMetaData_Header_Prefix,
        new ArrayList<String>(Arrays.asList("value1_1", "value1_2"))); // valid prefix as suffix
    userMetadata.put(RestUtils.Headers.UserMetaData_Header_Prefix + "key4", new ArrayList<String>()); // empty list
    RestUtils.setAmbryHeaders(headers, userMetadata);
    verifyBlobPropertiesConstructionSuccess(headers);

    RestRequest restRequest = createRestRequest(RestMethod.POST, "/", headers);
    byte[] userMetadataByteArray = RestUtils.buildUsermetadata(restRequest);
    Map<String, List<String>> userMetadataMap = RestUtils.getUserMetadataFromByteArray(userMetadataByteArray);

    // key1, output should be same as input
    String key = RestUtils.Headers.UserMetaData_Header_Prefix + "key1";
    assertTrue("key1 not found in user metadata map ", userMetadataMap.containsKey(key));
    assertTrue("Value for key1 didnt match, input value " + userMetadata.get(key) + ", output value " + userMetadataMap
        .get(key), Utils.verifyListsForEquality(userMetadata.get(key), userMetadataMap.get(key)));

    // key2, should not be found in output
    assertFalse(
        "key2 found in user metadata map " + userMetadataMap.get(RestUtils.Headers.UserMetaData_Header_Prefix + "key2"),
        userMetadataMap.containsKey(RestUtils.Headers.UserMetaData_Header_Prefix + "key2"));

    // key3, should not be found in output
    assertFalse(
        "key3 found in user metadata map " + userMetadataMap.get(RestUtils.Headers.UserMetaData_Header_Prefix + "key3"),
        userMetadataMap.containsKey(RestUtils.Headers.UserMetaData_Header_Prefix + "key3"));

    key = RestUtils.Headers.UserMetaData_Header_Prefix + "key4";
    assertTrue("key4 not found in user metadata map ", userMetadataMap.containsKey(key));
    assertTrue("Value for key4 didnt match, input value " + userMetadata.get(key) + ", output value " + userMetadataMap
        .get(key), Utils.verifyListsForEquality(userMetadata.get(key), userMetadataMap.get(key)));
  }

  /**
   * Tests building of User Metadata with empty input
   * @throws Exception
   */
  @Test
  public void getEmptyUserMetadataInputTest()
      throws Exception {
    JSONObject headers = new JSONObject();
    setAmbryHeaders(headers, Long.toString(RANDOM.nextInt(10000)), Long.toString(RANDOM.nextInt(10000)),
        Boolean.toString(RANDOM.nextBoolean()), generateRandomString(10), "image/gif", generateRandomString(10));
    Map<String, List<String>> userMetadata = new HashMap<String, List<String>>();
    RestUtils.setAmbryHeaders(headers, userMetadata);
    verifyBlobPropertiesConstructionSuccess(headers);

    RestRequest restRequest = createRestRequest(RestMethod.POST, "/", headers);
    byte[] userMetadataByteArray = RestUtils.buildUsermetadata(restRequest);
    Map<String, List<String>> userMetadataMap = RestUtils.getUserMetadataFromByteArray(userMetadataByteArray);
    assertTrue("UserMetadata should have been empty " + userMetadataMap, userMetadataMap.size() == 0);
  }

  @Test
  public void getHeaderValueFromListTest() {
    ArrayList<String> inputValue = new ArrayList<String>(Arrays.asList("value1_1", "value1_2"));
    String headerValue = RestUtils.getHeaderValueFromList(inputValue);
    String expectedValue = "[\"value1_1\",\"value1_2\"]";
    assertTrue("OutputValue \"" + headerValue + "\" is different from expected value \"" + expectedValue + "\"",
        headerValue.equals(expectedValue));
  }

  @Test
  public void getListFromHeaderValueTest() {
    String inputString = "[\"value1_1\",\"value1_2\"]";
    ArrayList<String> expectedValue = new ArrayList<String>(Arrays.asList("value1_1", "value1_2"));
    ArrayList<String> outputValue = RestUtils.getListFromHeaderValue(inputString);
    assertTrue("Output value " + outputValue + " is different from input value " + expectedValue,
        Utils.verifyListsForEquality(outputValue, expectedValue));
  }

  // helpers.
  // general.

  /**
   * Method to easily create {@link RestRequest} objects containing a specific request.
   * @param restMethod the {@link RestMethod} desired.
   * @param uri string representation of the desired URI.
   * @param headers any associated headers as a {@link org.json.JSONObject}.
   * @return A {@link RestRequest} object that defines the request required by the input.
   * @throws JSONException
   * @throws UnsupportedEncodingException
   * @throws URISyntaxException
   */
  private RestRequest createRestRequest(RestMethod restMethod, String uri, JSONObject headers)
      throws JSONException, UnsupportedEncodingException, URISyntaxException {
    JSONObject request = new JSONObject();
    request.put(MockRestRequest.REST_METHOD_KEY, restMethod);
    request.put(MockRestRequest.URI_KEY, uri);
    if (headers != null) {
      request.put(MockRestRequest.HEADERS_KEY, headers);
    }
    return new MockRestRequest(request, null);
  }

  /**
   * Generates a string of size {@code length} with random characters from {@link #ALPHABET}.
   * @param length the length of random string required.
   * @return a string of size {@code length} with random characters from {@link #ALPHABET}.
   */
  public String generateRandomString(int length) {
    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
    }
    return new String(text);
  }

  /**
   * Sets headers that helps build {@link BlobProperties} on the server. See argument list for the headers that are set.
   * Any other headers have to be set explicitly.
   * @param headers the {@link JSONObject} where the headers should be set.
   * @param contentLength sets the {@link RestUtils.Headers#Blob_Size} header.
   * @param ttlInSecs sets the {@link RestUtils.Headers#TTL} header.
   * @param isPrivate sets the {@link RestUtils.Headers#Private} header. Allowed values: true, false.
   * @param serviceId sets the {@link RestUtils.Headers#Service_Id} header.
   * @param contentType sets the {@link RestUtils.Headers#Content_Type} header.
   * @param ownerId sets the {@link RestUtils.Headers#Owner_Id} header. Optional - if not required, send null.
   * @throws JSONException
   */
  private void setAmbryHeaders(JSONObject headers, String contentLength, String ttlInSecs, String isPrivate,
      String serviceId, String contentType, String ownerId)
      throws JSONException {
    headers.putOpt(RestUtils.Headers.Blob_Size, contentLength);
    headers.putOpt(RestUtils.Headers.TTL, ttlInSecs);
    headers.putOpt(RestUtils.Headers.Private, isPrivate);
    headers.putOpt(RestUtils.Headers.Service_Id, serviceId);
    headers.putOpt(RestUtils.Headers.Content_Type, contentType);
    headers.putOpt(RestUtils.Headers.Owner_Id, ownerId);
  }

  /**
   * Verifies that a request with headers defined by {@code headers} builds {@link BlobProperties} successfully and
   * matches the values of the properties with those in {@code headers}.
   * @param headers the headers that need to go with the request that is used to construct {@link BlobProperties}.
   * @throws Exception
   */
  private void verifyBlobPropertiesConstructionSuccess(JSONObject headers)
      throws Exception {
    RestRequest restRequest = createRestRequest(RestMethod.POST, "/", headers);
    BlobProperties blobProperties = RestUtils.buildBlobProperties(restRequest);
    assertEquals("Blob size does not match", headers.getLong(RestUtils.Headers.Blob_Size),
        blobProperties.getBlobSize());
    long expectedTTL = Utils.Infinite_Time;
    if (headers.has(RestUtils.Headers.TTL) && !JSONObject.NULL.equals(headers.get(RestUtils.Headers.TTL))) {
      expectedTTL = headers.getLong(RestUtils.Headers.TTL);
    }
    assertEquals("Blob TTL does not match", expectedTTL, blobProperties.getTimeToLiveInSeconds());
    boolean expectedIsPrivate = false;
    if (headers.has(RestUtils.Headers.Private) && !JSONObject.NULL.equals(headers.get(RestUtils.Headers.Private))) {
      expectedIsPrivate = headers.getBoolean(RestUtils.Headers.Private);
    }
    assertEquals("Blob isPrivate does not match", expectedIsPrivate, blobProperties.isPrivate());
    assertEquals("Blob service ID does not match", headers.getString(RestUtils.Headers.Service_Id),
        blobProperties.getServiceId());
    assertEquals("Blob content type does not match", headers.getString(RestUtils.Headers.Content_Type),
        blobProperties.getContentType());
    if (headers.has(RestUtils.Headers.Owner_Id) && !JSONObject.NULL.equals(headers.get(RestUtils.Headers.Owner_Id))) {
      assertEquals("Blob owner ID does not match", headers.getString(RestUtils.Headers.Owner_Id),
          blobProperties.getOwnerId());
    }
  }

  /**
   * Verifies that a request with headers defined by {@code headers} builds UserMetadata successfully and
   * matches the values with those in {@code headers}.
   * @param headers the headers that need to go with the request that is used to construct the User Metadata
   * @throws Exception
   */
  private void verifyUserMetadataConstructionSuccess(JSONObject headers, Map<String, List<String>> inputUserMetadata)
      throws Exception {
    RestRequest restRequest = createRestRequest(RestMethod.POST, "/", headers);
    byte[] userMetadata = RestUtils.buildUsermetadata(restRequest);
    Map<String, List<String>> userMetadataMap = RestUtils.getUserMetadataFromByteArray(userMetadata);
    assertEquals("Total number of entries doesnt match ", inputUserMetadata.size(), userMetadataMap.size());
    for (String key : userMetadataMap.keySet()) {
      boolean keyFromInputMap = inputUserMetadata.containsKey(key);
      assertTrue("Key " + key + " not found in input user metadata", keyFromInputMap);
      List<String> outPutMapValue = userMetadataMap.get(key);
      List<String> inputMapValue = inputUserMetadata.get(key);
      boolean valueMatch = Utils.verifyListsForEquality(inputMapValue, outPutMapValue);
      assertTrue("Values didn't match for key " + key + ", value from input map value " + Arrays
          .toString(inputMapValue.toArray()) + ", and output map value " + Arrays.toString(outPutMapValue.toArray()),
          valueMatch);
    }
  }

  // getBlobPropertiesVariedInputTest() helpers.

  /**
   * Verifies that {@link RestUtils#buildBlobProperties(RestRequest)} fails if given a request with bad headers.
   * @param headers the headers that were provided to the request.
   * @param expectedCode the expected {@link RestServiceErrorCode} because of the failure.
   * @throws JSONException
   * @throws UnsupportedEncodingException
   * @throws URISyntaxException
   */
  private void verifyBlobPropertiesConstructionFailure(JSONObject headers, RestServiceErrorCode expectedCode)
      throws JSONException, UnsupportedEncodingException, URISyntaxException {
    try {
      RestRequest restRequest = createRestRequest(RestMethod.POST, "/", headers);
      RestUtils.buildBlobProperties(restRequest);
      fail("An exception was expected but none were thrown");
    } catch (RestServiceException e) {
      assertEquals("Unexpected RestServiceErrorCode", expectedCode, e.getErrorCode());
    }
  }

  /**
   * Adds extra values for the header {@code extraValueHeader} and tests that the right exception is thrown.
   * @param headers the headers that need to go with the request that is used to construct {@link BlobProperties}.
   * @param extraValueHeader the header for which extra values will be added.
   * @throws JSONException
   * @throws UnsupportedEncodingException
   * @throws URISyntaxException
   */
  private void tooManyValuesTest(JSONObject headers, String extraValueHeader)
      throws JSONException, UnsupportedEncodingException, URISyntaxException {
    String uri = "?" + extraValueHeader + "=extraVal1&" + extraValueHeader + "=extraVal2";
    try {
      RestRequest restRequest = createRestRequest(RestMethod.POST, uri, headers);
      RestUtils.buildBlobProperties(restRequest);
      fail("An exception was expected but none were thrown");
    } catch (RestServiceException e) {
      assertEquals("Unexpected RestServiceErrorCode", RestServiceErrorCode.InvalidArgs, e.getErrorCode());
    }
  }
}
