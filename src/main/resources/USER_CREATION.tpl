<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Notification Email</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f4f4f4; padding: 20px;">
        <tr>
            <td align="center">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden;">
                    <!-- Header -->
                    <tr>
                        <td align="center" style="background-color: #e60000; padding: 20px; color: #ffffff; font-size: 24px; font-weight: bold;">
                            Merchant Status Notification
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding: 20px; color: #333333; font-size: 16px;">
                            <p>Dear ${firstname}${lastName},</p>
                            <p>Kindly note that you have been successfully registered on the CAMS Portal.</p>
                            <p style="font-size: 18px; font-weight: bold; color: #28a745;">Status: REGISTERED</p>
                            <p>Please Login using the link below:</p>
                            <p align="center">
                                <a href="${url}" style="background-color: #e60000; color: white; padding: 12px 20px; font-size: 16px; text-decoration: none; display: inline-block;">View Document</a>
                            </p>
                            <p>If you have any questions, feel free to contact us.</p>
                            <p>Best regards,<br/>UBA - United Bank of Africa</p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td align="center" style="background-color: #f4f4f4; padding: 10px; font-size: 12px; color: #666666;">
                            &copy; 2025 UBA - United Bank of Africa. All rights reserved.<br>
                            This is an automated message, please do not reply.
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>