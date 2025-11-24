# Carrier Auto-Detection Feature

## Overview
The system now supports automatic carrier detection for tracking numbers through 17Track API integration. Users can add tracking numbers without specifying the carrier, and the system will automatically identify and save the correct carrier information.

## How It Works

### Backend Flow

1. **User adds tracking number** (with or without carrier code)
2. **System registers to 17Track** with `carrier=0` (auto-detect mode)
3. **System queries 17Track** to get carrier information
4. **17Track responds** with carrier ID and carrier name
5. **System converts** carrier ID to standard code (e.g., 1001 → "ups")
6. **System saves** tracking number with actual carrier information

### Carrier ID Mapping

The system supports automatic detection for the following carriers (based on 17Track official carrier list):

| Carrier | 17Track ID | System Code | Notes |
|---------|-----------|-------------|-------|
| UPS | 100002 | ups | Main UPS service |
| UPS Mail Innovations | 100398 | ups-mail-innovations | |
| UPS Freight | 100399 | ups-freight | |
| FedEx | 100003 | fedex | Main FedEx service |
| FedEx Intl | 100222 | fedex-intl | International Connect |
| USPS | 21051 | usps | US Postal Service |
| DHL Express | 100001 | dhl | Main DHL service |
| DHL Paket | 7041 | dhl-paket | |
| DHL eCommerce US | 7047 | dhl-ecommerce-us | |
| DHL eCommerce Asia | 7048 | dhl-ecommerce-asia | |
| DHL eCommerce CN | 100765 | dhl-ecommerce-cn | |
| 4PX | 190094 | 4px | |
| China Post | 3011 | china-post | |
| China EMS | 3013 | china-ems | |
| Yanwen | 190012 | yanwen | |
| SF Express | 100012 | sf-express | 顺丰速运 |
| Cainiao | 190271 | cainiao | 菜鸟 |
| ZTO International | 190175 | zto | 中通国际 |

**Reference**: Full carrier list available at https://res.17track.net/asset/carrier/info/apicarrier.all.json (3000+ carriers)

## Configuration

### 17Track API Settings
File: `src/main/resources/application.yml`

```yaml
track17:
  api:
    url: https://api.17track.net/track/v2.4
    token: 872063955E597617098184FDD971DD16
    register-endpoint: /register
    query-endpoint: /gettrackinfo
    timeout: 10000
```

**Note:** The token is a test account token. Replace with production token before deployment.

## Usage

### Frontend - Adding Single Tracking Number

1. Click "添加运单" button
2. Enter tracking number
3. Leave carrier dropdown as "自动识别" or select empty option
4. Click submit
5. System will automatically detect and save carrier information

### Frontend - Batch Import

CSV format supports empty carrier field for auto-detection:

```csv
运单号,承运商代码,备注
1Z999AA10123456784,ups,示例运单1
123456789012,,自动识别承运商
9400100000000000000000,usps,示例运单3
```

Second row shows empty carrier field - system will auto-detect.

### API Usage

**POST** `/api/v1/tracking`

Request body:
```json
{
  "trackingNumber": "1Z999AA10123456784",
  "carrierCode": "",
  "source": "manual",
  "remarks": "Test package"
}
```

Leave `carrierCode` empty or null for auto-detection.

**POST** `/api/v1/tracking/batch-import`

Request body:
```json
{
  "items": [
    {
      "trackingNumber": "1Z999AA10123456784",
      "carrierCode": "",
      "remarks": "Auto-detect carrier"
    }
  ]
}
```

## Implementation Details

### Backend Files Modified

1. **TrackingRequest.java** - Made `carrierCode` optional
2. **BatchImportItem.java** - Made `carrierCode` optional
3. **Track17RegisterRequest.java** - Returns `carrier=0` when code is blank
4. **TrackingService.java** - Added auto-detection logic in `create()` and `batchImport()`
5. **Track17Service.java** - Proper v2.4 API integration
6. **Track17QueryResponse.java** - Complete response structure

### Key Methods

**TrackingService.java:47-132**
- Registers tracking to 17Track
- Queries to get carrier information
- Extracts carrier name (w1) and carrier ID
- Converts carrier ID to system code
- Saves tracking with actual carrier

**TrackingService.java:137-151**
- `convertCarrierIdToCode()` - Maps 17Track carrier IDs to system codes

**TrackingService.java:359-442**
- `batchImport()` - Batch import with auto-detection support

### Frontend Files Modified

**Tracking.vue**
- Line 198: Added "自动识别" option in carrier dropdown
- Line 195: Placeholder text "留空则自动识别"
- Line 292: Updated help text about auto-detection
- Line 615: Allows empty carrier in CSV parsing
- Line 650-652: CSV template with auto-detect example

## Testing

### Test Scenarios

1. **Auto-detect UPS tracking**
   - Tracking: `1Z999AA10123456784`
   - Expected: carrier = "ups", carrierName = "UPS"

2. **Auto-detect USPS tracking**
   - Tracking: `9400100000000000000000`
   - Expected: carrier = "usps", carrierName = "USPS"

3. **Manual carrier specification**
   - Tracking: `1Z999AA10123456784`
   - Carrier: "ups"
   - Expected: Uses specified carrier, still queries for carrierName

4. **Batch import with mixed carriers**
   - Some with carrier specified, some without
   - Expected: Auto-detects empty ones, uses specified ones

### Verification Steps

1. **Start the backend server**
   ```bash
   mvn spring-boot:run
   ```

2. **Check 17Track API configuration**
   - Verify token is configured in `application.yml`
   - Check logs for successful API connection

3. **Test single tracking add**
   - Open frontend: http://localhost:3000
   - Add tracking number without carrier
   - Check database for saved carrier information

4. **Test batch import**
   - Download CSV template
   - Add tracking numbers with empty carrier field
   - Upload and verify results

5. **Check logs**
   ```
   17Track identified carrier: UPS (ups)
   Tracking number created successfully: 123
   ```

## Error Handling

The system gracefully handles 17Track API failures:

- If registration fails: Continues with carrier="unknown"
- If query fails: Uses user-specified carrier or "unknown"
- Logs warning but doesn't block tracking creation
- Tracking can be synced later to update carrier info

## Troubleshooting

### Carrier shows as "unknown"

**Possible causes:**
1. 17Track API token is invalid or expired
2. Tracking number not recognized by 17Track
3. Network connectivity issues
4. Tracking number is too new (not in carrier system yet)

**Solution:**
- Check application.yml for valid token
- Verify tracking number is valid
- Wait a few hours and try syncing again
- Check backend logs for specific error messages

### Auto-detection not working

**Possible causes:**
1. Frontend not sending empty string for carrier
2. Backend validation rejecting empty carrier
3. 17Track API returning error

**Solution:**
- Check browser network tab for request payload
- Verify backend logs show "carrier: auto"
- Test using direct API call with Postman/curl

## Production Checklist

- [ ] Replace test 17Track token with production token
- [ ] Set up error monitoring for 17Track API failures
- [ ] Configure retry logic for failed auto-detections
- [ ] Set up scheduled job to retry "unknown" carriers
- [ ] Monitor auto-detection success rate
- [ ] Document unsupported carrier handling
- [ ] Set up alerts for high failure rates

## Additional Notes

- Auto-detection requires active internet connection to 17Track API
- First query may take longer as 17Track processes new tracking number
- Recommend waiting 1-2 hours after package ships for best results
- System stores both carrier code and carrier name from 17Track
- Carrier can be manually corrected later if auto-detection fails
