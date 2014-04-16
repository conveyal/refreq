# refreq

A tool for coercing a GTFS into a frequency-based format

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
