import firebase_admin
from firebase_admin import credentials, initialize_app, storage
import sqlite3


DATABASE = './database.db'

def get_db():
    return sqlite3.connect(DATABASE)


def if_table_doesnt_exist_create_table():
    conn =  get_db()
    cursor = conn.cursor()
    sql = """create table if not exists AudioTracker(
        id INTEGER NOT NULL PRIMARY KEY,
        imei TEXT NOT NULL,
        latitude TEXT NOT NULL,
        longitude TEXT NOT NULL,
        datetime TEXT NOT NULL,
        audio BLOB NOT NULL
    );"""
    cursor.execute(sql)
    conn.commit()
    cursor.close()
    conn.close()

def fetch_data_from_firebase_and_save_to_db():
    locations = []
    if not firebase_admin._apps:
        cred = credentials.Certificate('key.json') 
        initialize_app(cred, {"storageBucket": "audiotracker-f9fd1.appspot.com"})
    
    bucket = storage.bucket()
    blobs = bucket.list_blobs()

    for blob in blobs:
        filename = blob.name.split("/")[-1]
        splitted_by_underscore = filename.split('_')
        audio_byte_data = bucket.get_blob(blob.name).download_as_bytes()
        with open(filename, "wb") as f:
            f.write(audio_byte_data)
            if_table_doesnt_exist_create_table()
        
        conn = get_db()
        cursor =  conn.cursor()
        sql = """insert into AudioTracker ('imei', 'longitude', 'latitude', 'datetime', 'audio')
        values (?,?,?,?,?);"""
        data_tuple = (
            splitted_by_underscore[0],
            splitted_by_underscore[1],
            splitted_by_underscore[2],
            splitted_by_underscore[3].split(".")[0],
            audio_byte_data
        )
        cursor.execute(sql, data_tuple)
        conn.commit()
        cursor.close()
        conn.close()
        locations.append((splitted_by_underscore[2], splitted_by_underscore[1]))

    return locations
