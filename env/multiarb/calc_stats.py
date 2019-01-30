#!/usr/bin/env python3

import numpy as np

latency = [0] * 8
count = [0] * 8
delay = [0] * 8
filename = "bench.log"

with open(filename, "r") as fh:
    for fd in fh:
        if fd.find("STAT") == 0:
            items = fd.strip().split()
            if items[1] == "latency":
                latency[int(items[2])] = float(items[3])
            elif items[1] == "count":
                count[int(items[2])] = float(items[3])
            elif items[1] == "delay":
                delay[int(items[2])] = float(items[3])

latency = np.array(latency)
count = np.array(count)
delay = np.array(delay)

avg_lat = latency / count
load = count / (count + delay)

print("Latency=",np.mean(avg_lat))
print("Accepted Load=", np.mean(load))
