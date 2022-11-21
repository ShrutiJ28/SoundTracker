from pydub import AudioSegment
import numpy as np
import soundfile as sfile

audio = AudioSegment.from_mp3(
    "52ea4db6af82d569_-8.50471773 _51.87808243 _Nov 21, 2022 1:40:51 AM.mp3"
)
signal, sr = sfile.read(audio)
samples = audio.get_array_of_samples()
samples_sf = 0
try:
    samples_sf = signal[:, 0]  # use the first channel for dual
except:
    samples_sf = signal  # for mono


def convert_to_decibel(arr):
    ref = 1
    if arr != 0:
        return 20 * np.log10(abs(arr) / ref)

    else:
        return -60
