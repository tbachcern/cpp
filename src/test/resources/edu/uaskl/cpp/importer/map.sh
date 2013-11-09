#!/bin/sh
osmosis --read-pbf rheinland-pfalz-latest.osm.pbf \
        --bounding-polygon file="zweibruecken.poly" \
        --write-pbf zweibruecken_no_meta.osm.pbf omitmetadata=true
osmosis --read-pbf zweibruecken_no_meta.osm.pbf \
        --tf accept-ways highway=* \
        --tf reject-ways highway=motorway,motorway_link \
        --tf reject-ways waterway=* \
        --tf reject-relations type=election,waterway \
        --tf reject-nodes highway=bus_stop \
        --tf reject-nodes amenity=* \
        --tf reject-ways highway=footway,steps,path,track,cycleway \
        --tf reject-relations boundary=* \
        --tf reject-ways area=yes \
        --used-node \
        --write-xml zweibruecken_way_no_meta.osm
