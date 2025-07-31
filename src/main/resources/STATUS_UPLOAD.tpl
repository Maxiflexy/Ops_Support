<!-- Upload Notification Email -->
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Upload Notification</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
  <table width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f4f4f4; padding: 20px;">
    <tr>
      <td align="center">
        <table width="600" cellspacing="0" cellpadding="0" border="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden;">
          
          <!-- Header -->
          <tr>
            <td align="center" style="background-color: #ec4f30; padding: 20px; color: #ffffff; font-size: 22px; font-weight: bold;">
              Document Upload Notification
            </td>
          </tr>
          
          <!-- Body -->
          <tr>
            <td style="padding: 20px; color: #333333; font-size: 16px;">
              <p>Hello,</p>
              <p>This is to notify you that a new document has been successfully uploaded.</p>

              <table width="100%" cellspacing="0" cellpadding="8" border="0" style="margin: 15px 0; border: 1px solid #dddddd; border-radius: 4px;">
                <tr>
                  <td style="background-color: #f9f9f9; font-weight: bold;">Document ID:</td>
                  <td>${document_id}</td>
                </tr>
                <tr>
                  <td style="background-color: #f9f9f9; font-weight: bold;">Uploaded By:</td>
                  <td>${uploader_name}</td>
                </tr>
              </table>

              <p>If this was not expected or you have any concerns, please follow up accordingly.</p>

              <p>Best regards,<br/>
              UBA - United Bank of Africa</p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td align="center" style="background-color: #f4f4f4; padding: 10px; font-size: 12px; color: #666666;">
              &copy; 2025 UBA - United Bank of Africa. All rights reserved.<br/>
              This is an automated message. Please do not reply.
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
