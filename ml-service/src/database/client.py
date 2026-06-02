import os

from supabase import create_client, Client
from dotenv import load_dotenv

load_dotenv()

_url = os.getenv("SUPABASE_URL")
_key = os.getenv("SUPABASE_KEY")

if not _url or not _key:
    raise ValueError("Missing Supabase credentials in .env file")

supabase: Client = create_client(_url, _key)