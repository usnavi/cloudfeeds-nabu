cloudfeeds-nabu
===============
[Nabu](http://en.wikipedia.org/wiki/Nabu) <a href="http://www.mesopotamia.co.uk/gods/explore/images/nabu_p.gif"><img src="http://www.mesopotamia.co.uk/gods/explore/images/nabu_p.gif" width="5%" height="5%" alt="Nabu"/></a> is the Babylonian god of writing and wisdom, and is the patron of [the scribes, librarians and archivists](http://en.wikipedia.org/wiki/Preservation_%28library_and_archival_science%29#Antecedents). [cloudfeeds-nabu](http://github.com/rackerlabs/cloudfeeds-nabu) is the code repository for the Cloud Feeds Archiving solution. 

The Cloud Feeds Archiving solution deals with archiving cloud events that come through Rackspace Cloud Feeds' system. The whole solution is comprised of multiple sub components:

* [cloudfeeds-ballista](http://github.com/rackerlabs/cloudfeeds-ballista) is the component that is responsible for reading the Cloud Feeds events from our internal Relational Database and throwing them over to Hadoop FS on a daily basis.

* [Usmu](http://www.ancient.eu/article/221/) <a href="http://www.mesopotamia.co.uk/gods/explore/images/isimud_p.gif"><img src="http://www.mesopotamia.co.uk/gods/explore/images/isimud_p.gif" width="5%" height="5%" alt="Usmu"/></a> is the Akkadian messenger god of Ea. He was a two-faced god, representing his coming and going on errands. Here, [usmu](http://github.com/rackerlabs/cloudfeeds-nabu) is the component that reads the exported data of cloudfeeds-ballista and writing it to a partitioned Hive table.

* [Arazu](http://www.ancient.eu/article/221/) is the Babylonian god of completed construction. He was worshipped at the conclusion of building projects. Here, Arazu is the component that reads the Hive table, produces the tenant archive files, and writes these files in the customer's designated Cloud Files container(s).
