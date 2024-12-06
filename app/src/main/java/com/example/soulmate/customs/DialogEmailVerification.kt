import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.example.soulmate.R

class DialogEmailVerification(context: Context) : Dialog(context) {

    private var messageTextView: TextView

    init {
        setContentView(R.layout.dialog_email_verification)
        messageTextView = findViewById(R.id.tv_dialog_message) // Your TextView inside the dialog layout
    }

    fun setMessage(message: String) {
        messageTextView.text = message
    }
}
