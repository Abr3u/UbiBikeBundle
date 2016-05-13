#!/usr/bin/python
import sys
import time
import math

import telnetlib

import gpxpy
import gpxpy.gpx

# Syntax: ./experiment.py <stations waypoints>.gpx [<emulator port>:]<track.gpx>
# Example: ./experiment.py stations-1.gpx 5554:route-1.gpx
# The first argument should point to a file containing waypoints. 
# The next arguments (you can offer an arbitrary number of args) must contain
# two elements separated by a ':'. The first part MUST be one local emulator
# port. The second must point to a file containing a track.

def gps_generator(gpx):
	for track in gpx.tracks:
		for segment in track.segments:
			for point in segment.points:
				yield(point)
def syntax():
	print( "Syntax: ./experiment.py <stations>.gpx [<emulator port>:]<track.gpx> ...")
	
# Returns true if two GPS tuples are near (euclidean distance of the coordinates).
def is_near(loc1, loc2):
	return math.hypot(loc2.latitude-loc1.latitude, loc2.longitude-loc1.longitude) < 0.001

# Finds and informs the user of devices near to other devices or stations.
def find_near_locations(devs, stts):
	devs_names = list(devs.keys())
	devs_locs = list(devs.values())	
	stts_names = list(stts.keys())
	stts_locs = list(stts.values())
	for i in range(len(devs)):
		# Nearby devices
		for j in range(i+1, len(devs)):
			if is_near(devs_locs[i], devs_locs[j]):
				input('{0} is near to {1}.'.format(devs_names[i], devs_names[j]))
		# Nearby stations
		for j in range(len(stts)):
			if is_near(devs_locs[i], stts_locs[j]):
				input('{0} is near to {1}.'.format(devs_names[i], stts_names[j]))

# Uses telnet to update the GPS location of a single emulator.
def update_emulator_gps(emu, loc):
	string = "geo fix " + str(loc.latitude) + " " + str(loc.longitude) + "\r\n"
	tn=telnetlib.Telnet('localhost',emu)
	tn.write(string.encode('utf-8'))
	tn.read_until(b"ok", 1);
	tn.close()

if len(sys.argv) < 3:
	syntax()
	sys.exit()

# <track name, emulator port>
devs={}
# <track name, [gps generator, finished?]>
tracks={}
# <track name, GPX point>
dev_location={}
# <waypoint name, GPX point>
stt_location={}

gpx = gpxpy.parse(open(sys.argv[1], 'r'))
for waypoint in gpx.waypoints:
	stt_location[waypoint.name] = waypoint

for arg in sys.argv[2:]:
	elem = arg.split(":")
	if len(elem) == 2:
		emu_id = elem[0]
		track_file = elem[1]
	elif len(elem) == 1:
		track_file = elem[0]
	else:
		syntax()
		sys.exit()
	
	gpx = gpxpy.parse(open(track_file, 'r'))
	
	if len(gpx.tracks) > 1:
		print ('Error: file {0} has more than one track.' . format(track_file))
		sys.exit()
	track_name = gpx.tracks[0].name
	
	if len(elem) > 1:
		devs[track_name] = emu_id
	tracks[track_name] = [gps_generator(gpx), True]

active_tracks = len(tracks)
# Until no track is active (i.e. all devices are stopped)
while active_tracks > 0:
	for track_name in tracks:
		track = tracks[track_name]
		if not track[1]:
			continue
		try:
			new_loc = next(track[0])
			dev_location[track_name] = new_loc
			update_emulator_gps(devs[track_name], new_loc)
			print ('{0} in at {1}, {2} '.format(track_name, new_loc.latitude, new_loc.longitude))
		except StopIteration:
			track[1] = False
			active_tracks -= 1
			print ('Track {0} ended.'.format(track_name))
	find_near_locations(dev_location, stt_location)
	time.sleep(1)
