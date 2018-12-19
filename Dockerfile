FROM nuxeo/nuxeo:master

RUN cat /etc/nuxeo/nuxeo.conf.template > $NUXEO_CONF
ADD nuxeo-notification-stream-*/target/*SNAPSHOT.jar /opt/nuxeo/server/nxserver/bundles/