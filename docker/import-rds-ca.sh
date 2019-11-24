#!/usr/bin/env sh

OLDDIR="$PWD"

if [ -z "$CACERTS_FILE" ]; then
    CACERTS_FILE=$JAVA_HOME/jre/lib/security/cacerts
fi


mkdir /tmp/rds-ca && cd /tmp/rds-ca


echo "Splitting RDS certificates..."

csplit -sk /usr/share/ca-certificates/rds-combined-ca-bundle.pem "/-BEGIN CERTIFICATE-/" "{$(grep -c 'BEGIN CERTIFICATE' /usr/share/ca-certificates/rds-combined-ca-bundle.pem | awk '{print $1 - 2}')}"

for CERT in xx*; do
    # extract a human-readable alias from the cert
    ALIAS=$(openssl x509 -noout -text -in $CERT |
                   perl -ne 'next unless /Subject:/; s/.*CN=//; print')
    echo "importing $ALIAS into $CACERTS_FILE"
    # import the cert into the default java keystore
    keytool -import \
            -keystore  $CACERTS_FILE \
            -storepass changeit -noprompt \
            -alias "$ALIAS" -file $CERT
done

cd "$OLDDIR"

rm -r /tmp/rds-ca