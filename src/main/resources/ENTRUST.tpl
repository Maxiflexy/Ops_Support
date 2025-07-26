<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ws="http://ws.waei.uba.com/">
   <soapenv:Header />
   <soapenv:Body>
      <NS1:authenticateToken xmlns:NS1="http://ws.waei.uba.com/">
         <request>
            <response>${token}</response>
            <userGroup />
            <username>${username}</username>
            <requesterId />
            <requesterIp />
         </request>
      </NS1:authenticateToken>
   </soapenv:Body>
</soapenv:Envelope>