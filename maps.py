import os
from flask import Flask, render_template
from firebase import fetch_data_from_firebase_and_save_to_db


app = Flask(__name__)
app.config["GMAP_API_KEY"] = os.getenv("GMAP_API_KEY", "")

colors = ["yellow", "red", "blue", "green", "purple", "black", "white", "pink"]


@app.route("/")
def map():
    zoom = 13
    markers = ""
    locations = fetch_data_from_firebase_and_save_to_db()

    i = 0
    for (latitude, longitude) in locations:
        markers += (
            f"&markers=color:{colors[i%len(colors)]}|label:{i+1}|{latitude},{longitude}"
        )

    return render_template(
        "maps.html",
        latitude=locations[0][0],
        longitude=locations[0][1],
        markers=markers,
        zoom=zoom,
    )


def drange(start, stop, step):
    r = start
    while r < stop:
        yield r
        r += step


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=3000, debug=True)
