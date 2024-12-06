import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.soulmate.R

class VerificationEmailDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_email_verification, null)
        builder.setView(view)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
        return builder.create()
    }
}
