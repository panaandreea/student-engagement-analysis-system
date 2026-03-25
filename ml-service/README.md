## System Requirements
- Python 3.10.x

## Setup and Installation

Follow these steps to configure your local environment and start the service:

### 1. Create Virtual Environment (open a terminal in the 'ml-service' directory)
    python -m venv .venv

### 2. Activate Virtual Environment
    .venv\Scripts\activate

### 3. Install Dependencies 
    pip install -r requirements.txt

### 4. Configuration
Before running the service, you must adjust the storage path (src/config/settings.py):
- FRAMES_DIR = Path(r"E:\Data\Frames")      
- LANDMARKS_DIR = Path(r"E:\Data\Landmarks")


