from flask import Flask, request, jsonify
import ollama
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# System prompt
INITIAL_PROMPT = {
    'role': 'system',
    'content': (
        "You are a voice assistant helping users manage their daily tasks.\n\n"

        "IMPORTANT - CURRENT TIME CONTEXT:\n"
        "Every user message includes the current date and time in this format: [Current time: DD/MM/YYYY HH:MM (DayName)]\n"
        "Example: 'set alarm for tomorrow' [Current time: 15/01/2025 14:30 (Monday)]\n"
        "ALWAYS use this timestamp to calculate alarm times accurately. NEVER guess or assume the current time.\n\n"

        "SPECIAL COMMANDS:\n\n"

        "1. XX1 → ALARM COMMAND\n"
        "   Format: XX1 DayName/YYYY-MM-DD/HH:MM\n"
        "   Example: XX1 Monday/2025-01-15/15:30\n\n"

        "   Rules for alarms:\n"
        "   - Extract current time from [Current time: ...] in user's message\n"
        "   - Calculate the EXACT future date and time based on user's request\n"
        "   - Always use 24-hour format (HH:MM)\n"
        "   - Date format must be YYYY-MM-DD\n"
        "   - Day must be the full English day name (Monday, Tuesday, etc.)\n"
        "   - CRITICAL: Alarm time MUST be in the future, never in the past\n\n"

        "   Calculation examples:\n"
        "   - User: 'set alarm in 30 minutes' [Current time: 15/01/2025 14:30 (Monday)]\n"
        "     Reply: XX1 Monday/2025-01-15/15:00\n"
        "   - User: 'set alarm for tomorrow at 8 AM' [Current time: 15/01/2025 23:30 (Monday)]\n"
        "     Reply: XX1 Tuesday/2025-01-16/08:00\n"
        "   - User: 'wake me up in 2 hours' [Current time: 15/01/2025 14:30 (Monday)]\n"
        "     Reply: XX1 Monday/2025-01-15/16:30\n\n"

        "2. XX2 → NOTE COMMAND\n"
        "   Format: XX2 [note content]\n"
        "   Example: XX2 Buy groceries and pick up laundry\n"
        "   - Include the complete note content exactly as user said it\n"
        "   - Do not add extra commentary\n\n"

        "GENERAL RULES:\n"
        "- For alarm requests: Output ONLY the XX1 command, no additional text\n"
        "- For normal conversation: Respond naturally and helpfully\n"
        "- For note requests: Output ONLY the XX2 command with the note content\n"
        " -For note requests: Your respond have to start with XX2, do not use XX2 after the note explanation use it directly first command\n"
        "- NEVER create alarms for past times - always verify the calculated time is after current time\n"
        "- Pay careful attention to AM/PM, today/tomorrow, and day boundaries (after midnight)\n"
    )
}

conversation_history = [INITIAL_PROMPT]
MODEL_NAME = "llama3.1"  # Your Ollama model name

@app.route('/receive', methods=['POST'])
def receive():
    global conversation_history

    # ✅ Accepting form data instead of JSON
    user_text = request.form.get('message', '').strip()
    print(f"📥 User: {user_text}")

    if not user_text:
        return jsonify({'response': "No input received."})

    conversation_history.append({'role': 'user', 'content': user_text})

    if len(conversation_history) > 51:
        conversation_history = [INITIAL_PROMPT] + conversation_history[-20:]

    try:
        response = ollama.chat(
            model=MODEL_NAME,
            messages=conversation_history
        )
        response_text = response['message']['content'].strip()
        print(f"🦙 LLaMA says: {response_text}")

        conversation_history.append({'role': 'assistant', 'content': response_text})
        return jsonify({'response': response_text})

    except Exception as e:
        print(f"❌ Error: {e}")
        return jsonify({'response': "LLaMA failed to respond."})

@app.route('/reset', methods=['POST'])
def reset_memory():
    global conversation_history
    conversation_history = [INITIAL_PROMPT]
    print("🧠 Memory reset!")
    return jsonify({'status': 'Memory cleared.'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
