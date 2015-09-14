<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:event="http://docs.rackspace.com/core/event"
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns="http://docs.rackspace.com/api/cloudfeeds/non-string-attrs"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0">
   <xsl:variable name="nonStringAttrsList">
      <schema key="http://docs.rackspace.com/event/atom-hopper/feedcount" version="1">
         <attributes>product/@count</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/autoscale" version="1">
         <attributes>product/@desiredCapacity,product/@currentCapacity</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/bigdata" version="1">
         <attributes>product/@numberServersInCluster,product/@aggregatedClusterDuration,product/@bandwidthIn,product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/bigdata" version="2">
         <attributes>product/@numberServersInCluster,product/@aggregatedClusterDuration,product/@bandwidthIn,product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/billing/consolidation" version="1">
         <attributes>product/@invoicedInExternalBillingSystem</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cbs/snapshot" version="1">
         <attributes>product/@snapshot</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/cbs" version="1">
         <attributes>product/@provisioned</attributes>
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
         <attributes>product/@disk,product/@freeops,product/@costops</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/queues" version="1">
         <attributes>product/@requestCount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/queues/bandwidth" version="1">
         <attributes>product/@bandwidthInPublic,product/@bandwidthInServiceNet,product/@bandwidthOutPublic,product/@bandwidthOutServiceNet</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="1">
         <attributes>product/@memory,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="2">
         <attributes>product/@memory,product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dbaas" version="3">
         <attributes>product/@memory,product/@storage,product/@isHAInstance</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/dcx/ip-address-association"
              version="1">
         <attributes>product/@primary</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dedicatedvcloud/serverlicenseandsupport"
              version="1">
         <attributes>product/@vCPUCount,product/@vRAM</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/dedicatedvcloud/serveroslicense"
              version="1">
         <attributes>product/@vCPUCount,product/@vRAM</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/domain" version="1">
         <attributes>product/@purchaseTenure,product/@isAutoRenew</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/emailapps_usage/domain"
              version="1">
         <attributes>product/@maxNumberOfMailboxes,product/@currentNumberOfMailboxes</attributes>
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
      <schema key="http://docs.rackspace.com/event/identity/user" version="1">
         <attributes>product/@migrated,product/@multiFactorEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/identity/user" version="2">
         <attributes>product/@migrated,product/@multiFactorEnabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/health-monitor" version="1">
         <attributes>product/@delay,product/@timeout,product/@attemptsBeforeDeactivation</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/lb" version="1">
         <attributes>product/@port,product/@timeout,product/@halfClose,product/@networkItemId,product/@minConnections,product/@maxConnections,product/@maxConnectionRate,product/@rateInterval,product/@connectionLogEnabled,product/@contentCachingEnabled,product/@sslTerminationEnabled,product/@secureTrafficOnly,product/@securePort</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/lbaas/node" version="1">
         <attributes>product/@port,product/@weight</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/lbaas" version="1">
         <attributes>product/@avgConcurrentConnections,product/@avgConcurrentConnectionsSsl,product/@avgConcurrentConnectionsSum,product/@bandWidthIn,product/@bandWidthInSsl,product/@publicBandWidthInSum,product/@bandWidthOut,product/@bandWidthOutSsl,product/@publicBandWidthOutSum,product/@numPolls,product/@numVips,product/@hasSSLConnection</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/maas" version="1">
         <attributes>product/@monitoringZones</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/maas" version="2">
         <attributes>product/@monitoringZones</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/newrelic/alert" version="1">
         <attributes>product/@supportCategory,product/@supportSubCategory</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/nova/ip" version="1">
         <attributes>product/@reserved</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/nova" version="1">
         <attributes>product/@isManaged,product/@bandwidthIn,product/@bandwidthOut</attributes>
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
      <schema key="http://docs.rackspace.com/event/RHEL" version="1">
         <attributes>product/@used</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/bandwidth" version="1">
         <attributes>product/@bandwidthIn,product/@bandwidthOut</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/image" version="1">
         <attributes>product/@sliceId</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/servers/ip" version="1">
         <attributes>product/@reserved</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/slice" version="1">
         <attributes>product/@managed,product/@imageId,product/@options,product/@huddleId,product/@serverId,product/@customerId,product/@flavorId</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers" version="1">
         <attributes>product/@flavor,product/@extraPublicIPs,product/@extraPrivateIPs,product/@isRedHat,product/@isMSSQL,product/@isMSSQLWeb,product/@isWindows,product/@isSELinux,product/@isManaged</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/hostserver" version="1">
         <attributes>product/@coreID,product/@serverID,product/@huddleID,slice/@id</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/servers/hostserver" version="2">
         <attributes>product/@coreID,product/@serverID,product/@huddleID,slice/@id</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/db" version="1">
         <attributes>product/@storage</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/email" version="1">
         <attributes>product/@numberOfMailboxes,product/@boxSize</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/metered" version="1">
         <attributes>product/@bandWidthOut,product/@requestCount,product/@computeCycles</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/netapp" version="1">
         <attributes>product/@storage,product/@numFiles</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/ssl" version="1">
         <attributes>product/@SSLenabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/sites/subscription" version="1">
         <attributes>product/@isNewAccount</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/ssl" version="1">
         <attributes>product/@sanLicenseCount,product/@purchaseTenure,product/@isAutoRenew</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/support/account/roles" version="1">
         <attributes>role/@roleId,role/@suppressNotifications</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/event/support/account/teams" version="1">
         <attributes>team/@teamNumber,team/@suppressNotifications,team/@previousTeamNumber</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget/explicit" version="1">
         <attributes>product/@same_int,product/@num_checks,product/@disabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget/multiple" version="1">
         <attributes>product/@same_int,product/@num_checks,product/@disabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="1">
         <attributes>product/@same_int,product/@num_checks,product/@disabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="2">
         <attributes>product/@same_int,product/@num_checks,product/@disabled</attributes>
      </schema>
      <schema key="http://docs.rackspace.com/usage/widget" version="3">
         <attributes>mixPublicPrivateAttributes/@privateAttribute3</attributes>
      </schema>
   </xsl:variable>
</xsl:stylesheet>
