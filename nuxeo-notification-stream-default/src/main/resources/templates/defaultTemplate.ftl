<html lang="en">

<body>
  <div style="margin:0; padding:0; background-color:#e9ecef;font-family:Arial,sans-serif;" marginheight="0" marginwidth="0">
    <center>
      <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" height="100%" style="background-color:#e9ecef;border-collapse:collapse; font-family:Arial,sans-serif;margin:0; padding:0; min-height:100% ! important; width:100% ! important;border:none;">
        <tbody>
          <tr>
            <td align="center" valign="top" style="border-collapse:collapse;margin:0;padding:15px;border-top:0;min-height:100%!important;width:100%!important">
              <table cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;border:none;width:100%">
                <tbody>
                  <tr>
                    <td style="background-color:#f7f7f7;border-bottom:1px dashed #e9ecef;padding:8px 20px;">
                      <p style="font-weight:bold;font-size:15px;margin:0;color:#000;">
                        ${Runtime.getProperty('org.nuxeo.ecm.product.name')}</p>
                    </td>
                  </tr>
                  <tr>
                    <td style="background-color:#fff;padding:8px 20px;"><br />
                      <p style="font-size:1.4em;padding:10vh 10vw;margin: 5vh 5vw;">${message}</p>
                      <br />
                      <p style="margin:0;font-size:13px;">
                        <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word!important;" href="${sourceUrl}">&#187;
                          Consult the document ${Fn.htmlEscape(sourceTitle)}</a>
                      </p><br />
                    </td>
                  </tr>
                  <tr>
                    <td style="background-color:#f7f7f7;border-top:1px dashed #e9ecef;text-align:center;padding:8px 20px;">
                      <div style="font-size:12px;color:#bbb;">
                        <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word!important;" href="${unsubUrl}">Unsubscribe</a>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </td>
          </tr>
        </tbody>
      </table>
    </center>
  </div>
</body>
<html>
