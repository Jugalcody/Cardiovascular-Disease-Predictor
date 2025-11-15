package cardio.analyser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Objects;

public class Dashboard extends AppCompatActivity {

    TextView question,result;
    AppCompatButton next;
    RadioGroup radioGroup;

    LottieAnimationView lottieAnimation;
    Interpreter interpreter;
    RelativeLayout resultlayout;
    RadioButton option1,option2,option3,option4;

    String[][] arrquestion;
    Float[] arranswer=new Float[7];
    String flag="opt";
    int counter=0;
    EditText value;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        try {
            interpreter = new Interpreter(loadModelFile());
            Log.d("TFLITE", "Model loaded successfully!");
        } catch (Exception e) {
            Log.e("TFLITE", "FAILED TO LOAD MODEL: " + e);
        }

        lottieAnimation=findViewById(R.id.searchlottie);

        getWindow().setStatusBarColor(getColor(R.color.primary));

        radioGroup=findViewById(R.id.radiogroup1);
        option1=findViewById(R.id.optbtn1);
        option2=findViewById(R.id.optbtn2);
        option3=findViewById(R.id.optbtn3);
        option4=findViewById(R.id.optbtn4);

        resultlayout=findViewById(R.id.predictor_result_layout);
        result=findViewById(R.id.predictor_result);

        value=findViewById(R.id.predictor_value);

        question=findViewById(R.id.header_predictor_heart_question);

        next=findViewById(R.id.nextbtn_predictor);

        arrquestion = new String[][]{
                {"How old are you?", "val","In years"},

                {"What is your gender?", "opt", "2", "Male", "Female"},

                {"What kind of chest pain do you feel?", "opt", "4",
                        "Pain during walking/exercise (Typical)",
                        "Uncertain or mixed pain (Atypical)",
                        "Not heart-related pain (Non-anginal)",
                        "No chest pain"},

                {"What is your resting blood pressure? (upper number)", "val","Systolic mm Hg"},

                {"What is your cholesterol level? (mg/dL)", "val","In mg/dL"},

                {"What is the highest heart rate you achieved? (or estimate)", "val","In BPM"},

                {"Do you get chest pain during exercise?", "opt", "2",
                        "No, I don’t get pain",
                        "Yes, I get pain while exercising"}
        };

        loadquestion();

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    if (counter>= arrquestion.length) {
                      showresult();
                    } else {
                        if (flag.equals("opt")) {
                            if (radioGroup.getCheckedRadioButtonId() != -1) {

                                if (option1.isChecked()) arranswer[counter] = 0f;
                                else if (option2.isChecked()) arranswer[counter] = 1f;
                                else if (option3.isChecked()) arranswer[counter] = 2f;
                                else if (option4.isChecked()) arranswer[counter] = 3f;
                                counter++;
                                if (counter>= arrquestion.length) {
                                    showresult();
                                }
                                else {
                                    loadquestion();
                                }
                            } else {
                                Toast.makeText(Dashboard.this, "Please select an option!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            String val = value.getText().toString().trim();
                            if(val.isEmpty()) {
                                Toast.makeText(Dashboard.this, "Enter value!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            arranswer[counter] = Float.parseFloat(val);
                            value.setText("");
                            counter++;
                            if (counter>= arrquestion.length) {
                                showresult();
                            }
                            else {
                                loadquestion();
                            }
                        }


                    }
                } catch (Exception e) {
                    Toast.makeText(Dashboard.this,e.toString(), Toast.LENGTH_LONG).show();
                }
            }

            private void showresult() {
                question.setVisibility(View.GONE);
                radioGroup.setVisibility(View.GONE);
                value.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                lottieAnimation.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resultlayout.setVisibility(View.VISIBLE);
                        lottieAnimation.setVisibility(View.GONE);
                    }
                }, 2000);


                try {
                    float perform = predict(
                            arranswer[0],   // age
                            arranswer[1],   // gender
                            arranswer[2],   // chest pain type
                            arranswer[3],   // resting bp
                            arranswer[4],   // cholesterol
                            arranswer[5],   // max heart rate
                            arranswer[6]
                    );
                    //result.setText(String.valueOf(perform));
                    if (perform>=0.8) {
                        result.setText("Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                "\n\nResult is not good, you may have heart related issues in future, be careful!");

                    } else {
                        if(perform>0.5 && perform<0.8){
                            result.setText("Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                    "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                    "\n\nYour heart is moderately healthy, need to be careful!");
                        }
                        else{
                            result.setText("Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                    "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                    "\n\nYour heart is very healthy!");
                        }

                    }
                } catch (Exception e) {
                    result.setText(e.toString());
                    Toast.makeText(Dashboard.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }

        });

    }
    private float predict(float age,
                          float gender,
                          float cp,
                          float trestbps,
                          float chol,
                          float thalach,
                          float exang) {

        float[][] input = {{
                age, gender, cp, trestbps, chol, thalach, exang
        }};

        float[][] output = new float[1][1];
        interpreter.run(input, output);
        return output[0][0];
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("heart_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    private void loadquestion() {

        int i=counter;
        radioGroup.clearCheck();
        if (i <=arrquestion.length - 1) {

            if(i==arrquestion.length - 1){
                next.setText("Predict");
            }

            question.setText((i+1) + ". " + arrquestion[i][0]);
            if (arrquestion[i][1].equals("opt")) {
                flag = "opt";
                value.setVisibility(View.GONE);
                radioGroup.setVisibility(View.VISIBLE);
                int num = Integer.parseInt(arrquestion[i][2].trim());
                option1.setText(arrquestion[i][3]);
                if (num == 2) {
                    option2.setText(arrquestion[i][4]);
                    option3.setVisibility(View.GONE);
                    option4.setVisibility(View.GONE);
                    option2.setVisibility(View.VISIBLE);

                } else if (num == 3) {
                    option2.setText(arrquestion[i][4]);
                    option3.setText(arrquestion[i][5]);
                    option4.setVisibility(View.GONE);
                    option3.setVisibility(View.VISIBLE);
                } else if (num == 4) {
                    option2.setText(arrquestion[i][4]);
                    option3.setText(arrquestion[i][5]);
                    option4.setText(arrquestion[i][6]);
                    option4.setVisibility(View.VISIBLE);
                    option3.setVisibility(View.VISIBLE);
                }
            } else {
                flag = "val";
                value.setHint(arrquestion[i][2]);
                value.setVisibility(View.VISIBLE);
                radioGroup.setVisibility(View.GONE);
            }
        }
        else{

        }
    }

}