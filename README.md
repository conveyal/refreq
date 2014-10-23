# refreq

A tool for coercing a GTFS into a frequency-based format. Refreq does this by making several groups, each with members that match a line in the 'modfile', establishing a representitive frequency for those set of trips, and then writing a 'frequences.txt' record for each group pointing to new trip averaged from the grouped trips.

## prereqs

- Gradle

## build

    $ gradle fatJar
    
## running

    $ java -jar ./build/libs/refreq.jar gtfs_feed_path sample_date [mod_file]
    
With parameters:
- gtfs_feed_path: A path to a gtfs feed.
- sample_date: Sample date in YYYYMMDD file. The date that you want your fake GTFS frequencies to superficially resemble.
- mod_file: optional. A service modification file as output by https://gist.github.com/bmander/10792620

The mod_file is a CSV with the following fields. The header line is not optional.

- 'route'. Either 'route' or 'trip' are required. The route_short_name of trips to group into a frequency subschedule.
- 'trip'. Either 'route' or 'trip' are required. A single trip to build a frequency subschedule out of.

'absolute'. Optional. If the value is 'true', the contents of the time window fields will be taken as the absolute period of the frequency subschedule. If not, the contents will be taken as a multiplier on the representitive period.

The next several fields can take different values depending on whether the 'absolute' is set to 'true'. If not the values are either a decimal number indicating the change in the *period* of the frequency subschedule - that is, if the value is '2.0' then vehicles will arrive half as frequently, or '0.5' means twice as frequency. The string 'inf' means the period becomes infintely long - that is, service is cancelled. The string 'None' will result in no frequency subschedule for those trips at that time, but doesn't imply a service change. If the 'absolute' value is true these fields take an integer value.

- 'peak_am'. Optional. Frequency change or period from 6am to 9am.
- 'midday'. Optional. 9am to 3pm.
- 'peak_pm'. Optional. 3pm to 6pm.
- 'night'. Optional. 6pm to 12am.
- 'sat'. Optional. 9am to 12am Saturday.
- 'sun'. Optional. 9am to 12am Sunday.

Finally two optional parameters that work together.

- 'offset'. Optional. Only valid if 'offsetStop' is set. The stop_times will be slid around to ensure that the trip arrives at a particular stop at the particular time set, in seconds since midnight.
- 'offsetStop'. Optional. Only valid of 'offset' is set. The stop to arrive at at the 'offset' time.

For example:

    trip,peak_am,absolute,offset,offset_stop
    2247,3600,true,7200,105
    2605,3600,true,8100,105

This takes trip '2247' and makes a frequency-based schedule arriving every hour between 6am and 9am, where trip 2247 arrives at stop 105 exactly 7200 seconds.

Another example:

    route,peak_am,midday,peak_pm,night,sat,sun
    216,1.0,None,1.0,None,None,None
    217,inf,None,inf,None,None,None
    214,1.69230769231,None,1.92307692308,None,None,None

In this example, the route with the short name '214' (which is _not_ the route ID) will be summarized into a frequency-based schedule for the peak_am and peak_pm service windows, at which times its frequency will be somewhat reduced.
