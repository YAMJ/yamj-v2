#!/bin/sh
echo "Content-Type: text/javascript"
echo ""

# Extract the '&' delimited arguments from command line
wFile=${1%%&*}
wFile=${wFile//\%20/' '}
wAction=${1#*&}
wAction=${wAction%%&*}
wCallback=${1##*&}
wStatus=0

checkWatched()
{
if [ -f "$wFile" ]
then
 wStatus=1
fi
}

createWatched()
{
cat > "$wFile" <<EOF
 Watched
EOF
wStatus=1
}

deleteWatched()
{
 rm "$wFile" > /dev/null 2>&1
 wStatus=0
}

checkWatched

if [ $wAction -eq 1 ]
then
 if [ $wStatus -eq 1 ]
  then
   deleteWatched
  else
   createWatched
 fi
fi

echo "$wCallback($wAction,$wStatus);\n"
exit 0
