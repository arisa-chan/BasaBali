#!/usr/bin/env python3
"""Generates the five UI sound effects as 16-bit mono WAVs in res/raw/.
Each sound is spectrally and rhythmically distinct:
  click    – short high blip (tile tap)
  correct  – rising major triad chime
  wrong    – low descending buzz
  complete – ascending four-note fanfare
  fail     – slow descending minor cadence
"""
import math, os, struct, wave

SR = 44100

def tone(freq, dur, vol=0.55, harmonic=0.0, fade=3.0):
    n = int(SR * dur)
    out = []
    for i in range(n):
        t = i / SR
        x = math.sin(2 * math.pi * freq * t)
        if harmonic:
            x += harmonic * math.sin(4 * math.pi * freq * t)
        env = min(1.0, i / (SR * 0.005)) * math.exp(-fade * t / dur)
        out.append(x * env * vol)
    return out

def seq(parts):
    s = []
    for p in parts:
        s += tone(*p)
    return s

def write(name, samples):
    peak = max(abs(s) for s in samples) or 1.0
    samples = [s / peak * 0.7 for s in samples]
    f = int(SR * 0.004)
    for i in range(f):
        samples[i] *= i / f
        samples[-1 - i] *= i / f
    path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        'app', 'src', 'main', 'res', 'raw', name + '.wav')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    w = wave.open(path, 'wb')
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(b''.join(struct.pack('<h', int(max(-1.0, min(1.0, s)) * 32767)) for s in samples))
    w.close()
    print(f"{name}.wav  {len(samples)/SR:.2f}s  {os.path.getsize(path)//1024} KB")

write('click',    seq([(880.0, 0.05, 0.5, 0.0, 9.0)]))
write('correct',  seq([(523.25, 0.09, 0.55, 0.15, 2.0),
                      (659.25, 0.09, 0.55, 0.15, 2.0),
                      (783.99, 0.18, 0.60, 0.20, 3.0)]))
write('wrong',    seq([(233.08, 0.14, 0.5, 0.40, 2.5),
                      (174.61, 0.24, 0.5, 0.40, 3.0)]))
write('complete', seq([(523.25, 0.10, 0.55, 0.20, 2.0),
                      (659.25, 0.10, 0.55, 0.20, 2.0),
                      (783.99, 0.10, 0.55, 0.20, 2.0),
                      (1046.50, 0.34, 0.60, 0.25, 3.0)]))
write('fail',     seq([(392.00, 0.17, 0.5, 0.10, 2.0),
                      (329.63, 0.17, 0.5, 0.10, 2.0),
                      (261.63, 0.45, 0.5, 0.10, 3.5)]))
