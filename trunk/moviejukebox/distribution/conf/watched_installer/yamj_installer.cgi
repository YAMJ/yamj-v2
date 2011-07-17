#!/bin/sh
echo "Content-Type: text/javascript"
echo ""
if [ -d "/opt/sybhttpd/localhost.drives/SATA_DISK" ];
then
  find /opt/sybhttpd/localhost.drives/SATA_DISK/ -name "watched.cgi" -exec chmod 755 {} \;
fi

if [ -d "/opt/sybhttpd/localhost.drives/HARD_DISK" ];
then
  find /opt/sybhttpd/localhost.drives/HARD_DISK/ -name "watched.cgi" -exec chmod 755 {} \;
fi

echo "callBack();\n"
exit 0