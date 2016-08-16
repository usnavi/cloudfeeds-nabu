<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:event="http://docs.rackspace.com/core/event"
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns="http://docs.rackspace.com/api/cloudfeeds/non-string-attrs"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0">
   <xsl:variable name="nonStringAttrsList">
      <schema key="http://docs.rackspace.com/event/RHEL" version="1">
         <attributes>product/@used</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/atom-hopper/feedcount" version="1">
         <attributes>product/@count</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/autoscale" version="1">
         <attributes>product/@currentCapacity,product/@desiredCapacity</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/billing/consolidation" version="1">
         <attributes>product/@invoicedInExternalBillingSystem</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/dcx/ip-address-association"
              version="1">
         <attributes>product/@primary</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/domain" version="1">
         <attributes>product/@isAutoRenew,product/@purchaseTenure</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/identity/user" version="1">
         <attributes>product/@migrated,product/@multiFactorEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/identity/user" version="2">
         <attributes>product/@migrated,product/@multiFactorEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/health-monitor" version="1">
         <attributes>product/@attemptsBeforeDeactivation,product/@delay,product/@timeout</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/lb" version="1">
         <attributes>product/@connectionLogEnabled,product/@contentCachingEnabled,product/@halfClose,product/@maxConnectionRate,product/@maxConnections,product/@minConnections,product/@networkItemId,product/@port,product/@rateInterval,product/@securePort,product/@secureTrafficOnly,product/@sslTerminationEnabled,product/@timeout</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/node" version="1">
         <attributes>product/@port,product/@weight</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/newrelic/alert" version="1">
         <attributes>product/@supportCategory,product/@supportSubCategory</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/nova" version="1">
         <attributes>product/@bandwidthIn,product/@bandwidthOut,product/@isManaged</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers" version="1">
         <attributes>product/@extraPrivateIPs,product/@extraPublicIPs,product/@flavor,product/@isMSSQL,product/@isMSSQLWeb,product/@isManaged,product/@isRedHat,product/@isSELinux,product/@isWindows</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/bandwidth" version="1">
         <attributes>product/@bandwidthIn,product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/hostserver" version="1">
         <attributes>product/@coreID,product/@huddleID,slice/@id,product/@serverID</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/hostserver" version="2">
         <attributes>product/@coreID,product/@huddleID,slice/@id,product/@serverID</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/image" version="1">
         <attributes>product/@sliceId</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/slice" version="1">
         <attributes>product/@customerId,product/@flavorId,product/@huddleId,product/@imageId,product/@managed,product/@options,product/@serverId</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/ssl" version="1">
         <attributes>product/@isAutoRenew,product/@purchaseTenure,product/@sanLicenseCount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/support/account/roles" version="1">
         <attributes>role/@roleId,role/@suppressNotifications</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/support/account/teams" version="1">
         <attributes>team/@previousTeamNumber,team/@suppressNotifications,team/@teamNumber</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/bigdata" version="1">
         <attributes>product/@aggregatedClusterDuration,product/@bandwidthIn,product/@bandwidthOut,product/@numberServersInCluster</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/bigdata" version="2">
         <attributes>product/@aggregatedClusterDuration,product/@bandwidthIn,product/@bandwidthOut,product/@numberServersInCluster</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cbs" version="1">
         <attributes>product/@provisioned</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cbs/snapshot" version="1">
         <attributes>product/@snapshot</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudbackup/bandwidthIn"
              version="1">
         <attributes>product/@bandwidthIn</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudbackup/bandwidthOut"
              version="1">
         <attributes>product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudbackup/license" version="3">
         <attributes>product/@external</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudbackup/storage" version="1">
         <attributes>product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudfiles/bandwidth" version="1">
         <attributes>product/@bandwidthIn,product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudfiles/cdnbandwidth"
              version="1">
         <attributes>product/@cdnBandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cloudfiles/storage" version="1">
         <attributes>product/@costops,product/@disk,product/@freeops</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="1">
         <attributes>product/@memory,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="2">
         <attributes>product/@memory,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="3">
         <attributes>product/@isHAInstance,product/@memory,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dedicatedvcloud/serverlicenseandsupport"
              version="1">
         <attributes>product/@vCPUCount,product/@vRAM</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dedicatedvcloud/serveroslicense"
              version="1">
         <attributes>product/@vCPUCount,product/@vRAM</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/emailapps_usage/domain"
              version="1">
         <attributes>product/@currentNumberOfMailboxes,product/@maxNumberOfMailboxes</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/emailapps_usage/mailbox"
              version="1">
         <attributes>product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/emailapps_usage/subscription"
              version="1">
         <attributes>product/@isNewAccount,product/@maxNumberOfMailboxes</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/glance" version="1">
         <attributes>product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/lbaas" version="1">
         <attributes>product/@avgConcurrentConnections,product/@avgConcurrentConnectionsSsl,product/@avgConcurrentConnectionsSum,product/@bandWidthIn,product/@bandWidthInSsl,product/@bandWidthOut,product/@bandWidthOutSsl,product/@hasSSLConnection,product/@numPolls,product/@numVips,product/@publicBandWidthInSum,product/@publicBandWidthOutSum</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/lbaas" version="2">
         <attributes>product/@nonSslConnections,product/@publicBandWidthOut,product/@sslConnections</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/maas" version="1">
         <attributes>product/@monitoringZones</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/maas" version="2">
         <attributes>product/@monitoringZones</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/neutron/ip" version="1">
         <attributes>product/@public</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/neutron/ipbandwidth" version="1">
         <attributes>product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/nova/ip" version="1">
         <attributes>product/@reserved</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/queues" version="1">
         <attributes>product/@requestCount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/queues/bandwidth" version="1">
         <attributes>product/@bandwidthInPublic,product/@bandwidthInServiceNet,product/@bandwidthOutPublic,product/@bandwidthOutServiceNet</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/rackspacecdn/bandwidth"
              version="1">
         <attributes>product/@bandwidthOut,product/@sslEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/rackspacecdn/bandwidth"
              version="2">
         <attributes>product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/rackspacecdn/requestcount"
              version="1">
         <attributes>product/@requestCount,product/@sslEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/rackspacecdn/requestcount"
              version="2">
         <attributes>product/@requestCount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/servers/ip" version="1">
         <attributes>product/@reserved</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/db" version="1">
         <attributes>product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/email" version="1">
         <attributes>product/@boxSize,product/@numberOfMailboxes</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/metered" version="1">
         <attributes>product/@bandWidthOut,product/@computeCycles,product/@requestCount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/netapp" version="1">
         <attributes>product/@numFiles,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/ssl" version="1">
         <attributes>product/@SSLenabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/subscription" version="1">
         <attributes>product/@isNewAccount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="1">
         <attributes>product/@disabled,product/@num_checks,product/@same_int</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="2">
         <attributes>product/@disabled,product/@num_checks,product/@same_int</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="3">
         <attributes>mixPublicPrivateAttributes/@privateAttribute3</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget/explicit" version="1">
         <attributes>product/@disabled,product/@num_checks,product/@same_int</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget/multiple" version="1">
         <attributes>product/@disabled,product/@num_checks,product/@same_int</attributes>
      </schema>
   </xsl:variable>
</xsl:stylesheet>
