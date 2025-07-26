<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Notification Email</title>
  </head>
  <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4; color: #333333;">
    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%" style="background-color: #f4f4f4; padding: 20px 0;">
      <tr>
        <td align="center">
          <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="background-color: #ffffff; border-collapse: collapse;">
            <!-- Header -->
            <tr>
              <td align="center" style="background-color: #e60000; padding: 20px; color: #ffffff;">
                <h1 style="margin: 0; font-size: 24px;">Document Status Notification</h1>
              </td>
            </tr>
            <!-- Content -->
            <tr>
              <td style="padding: 20px;">
                <p style="margin: 0 0 10px; line-height: 1.6;">Dear ${initiator},</p>

                <p style="margin: 0 0 10px; line-height: 1.6;">
                  A file upload has been uploaded for <strong>${module_name}</strong> with request_id:
                  <strong>${document_id}</strong>
                </p>

                <p style="margin: 0 0 10px; line-height: 1.6;">
                  You can view the details of your document by logging into your account or clicking the button below:
                </p>

                <!-- Button -->
                <p style="text-align: center; margin: 20px 0;">
                  <a href="https://operation-support-project-infometics.vercel.app/login/"
                    style="background-color: #e60000; color: white; padding: 12px 20px; border-radius: 5px;
                    font-size: 16px; text-decoration: none; display: inline-block;">
                    Click here
                  </a>
                </p>

                <p style="margin: 0 0 10px; line-height: 1.6;">If you have any questions, feel free to contact us.</p>

                <p style="margin: 0; line-height: 1.6;">Best regards,<br />UBA - United Bank of Africa</p>
              </td>
            </tr>
            <!-- Footer -->
            <tr>
              <td align="center" style="background-color: #f4f4f4; padding: 10px; font-size: 12px; color: #666666;">
                <p style="margin: 0;">&copy; 2024 UBA - United Bank of Africa. All rights reserved.</p>
                <p style="margin: 0;">This is an automated message, please do not reply.</p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
