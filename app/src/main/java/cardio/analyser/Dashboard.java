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
    Float[] arranswer=new Float[11];
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
                {"What is your gender?", "opt", "2", "Male", "Female"},
                {"How tall are you? (cm)", "val", "In cm"},
                {"What is your weight? (kg)", "val", "In kg"},
                {"Resting BP high (systolic)?", "val", "mm Hg"},
                {"Resting BP low (diastolic)?", "val", "mm Hg"},
                {"Cholesterol level (mg/dL)?", "opt", "3","Less than 200 mg/dL","201 - 239 mg/dL","240 mg/dL or Higher"},
                {"What is your Blood sugar level?", "opt", "3", "Less than 100 mg/dL", "100 - 125 mg/dL","126 mg/dL or Higher"},
                {"Do you smoke?","opt","2","No","Yes"},
                {"Do you consume alcohol?","opt","2","No","Yes"},
                {"Do you exercise regularly?","opt","2","No","Yes"},
                {"Your age?","val","In years"},

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
                    float bmi=calBMI(arranswer[2],arranswer[1]);
                    float perform = predict(
                            arranswer[0],  // gender
                            arranswer[1],  // height
                            arranswer[2],  // weight
                            arranswer[3],  // ap_hi (resting BP high)
                            arranswer[4],  // ap_lo (resting BP low)
                            arranswer[5],  // cholesterol
                            arranswer[6],  // gluc
                            arranswer[7],  // smoke
                            arranswer[8],  // alco
                            arranswer[9],  // active
                            bmi, // BMI
                            arranswer[10]  // age_years
                    );

                    //result.setText(String.valueOf(perform));
                    String resulttxt="";
                    if (perform>=0.8) {
                        resulttxt="Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                "\n\nResult is not good, you may have heart related issues in future, be careful!";
                        resulttxt+="\n\nYour BMI is "+calBMI(arranswer[2],arranswer[1])+" kg/m^2 , ";
                        if(bmi<18.5){
                            resulttxt+="you are also an underweight person, focus on your diet as it can be dangerous if you continue this lifestyle.";
                        }
                        else if(bmi>18.5 && bmi<24.9){
                            resulttxt+="you are a healthy person according to your BMI. To remain healthy, you should do regular cardio exercises and intake healthy your diet with less fast foods intake especially the food with high cholesterol and high saturated fats.";
                        }
                        else{
                            resulttxt += "you are overweight according to your BMI, which may increase your risk of heart disease. Consider improving your overall diet i.e reduce regular intake of fast foods and start doing physical activities daily.";
                        }

                    } else {
                        if(perform>0.5 && perform<0.8){
                            resulttxt="Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                    "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                    "\n\nYour heart is moderately healthy, need to be careful!\n\n";
                            resulttxt+="\n\nYour BMI is "+calBMI(arranswer[2],arranswer[1])+" kg/m^2 , ";
                            if(bmi<18.5){
                                resulttxt+="you are also an underweight person, focus on your diet as it can be dangerous if you continue this lifestyle.";
                            }
                            else if(bmi>18.5 && bmi<24.9){
                                resulttxt+="you are a healthy person according to your BMI. To remain healthy, you should focus on regular cardio exercises and healthy diet intake. Most importantly reduce fast foods.";
                            }
                            else{
                                resulttxt += "you are overweight according to your BMI, which may increase your risk of heart disease. Consider improving your diet i.e reduce regular intake of fast foods and start doing physical activities daily.";
                            }
                        }
                        else{
                            resulttxt="Your healthy score is "+String.valueOf((1-perform)*100)+"%"+"\n"+
                                    "Disease Risk is "+String.valueOf((perform)*100)+"%"+
                                    "\n\nYour heart is very healthy!";
                            resulttxt+="\n\nYour BMI is "+calBMI(arranswer[2],arranswer[1])+" kg/m^2 , ";

                            if(bmi<18.5){
                                resulttxt+="you are an underweight person, focus on your diet as it can be dangerous if you continue this lifestyle. As your heart health is normal, you can focus on moderate fats and more protien but be careful of cholesterol level, you can do one thing, try eat apple before any risky food like chicken or dairy food like paneer as apple contains pectin that can reduce overall absorption of cholesterol in your gut";
                            }
                            else if(bmi>18.5 && bmi<24.9){
                                resulttxt+="you are a healthy person according to your BMI. You should focus on regular cardio exercises and healthy diet. Most importantly reduce your overall intake of fast foods especially which contains high saturated fats and high cholesterol";
                            }
                            else{
                                resulttxt += "you are overweight according to your BMI, which may increase your risk of heart disease. Consider improving your diet i.e reduce regular intake of fast foods especially which contains high saturated fats and high cholesterol, also start doing physical activities daily for overall health.";
                            }
                        }
                        resulttxt+="\n\nYou should focus add more fruits in your diet like pomegranate,apple,orange,pineapple,grapes,amla,etc which are very rich in vitamins,fibre,antioxidant and has anti-inflammatory properties that can keep your blood vessels healthy and protect you from any cardiovascular diseases";


                       result.setText(resulttxt);
                    }
                } catch (Exception e) {
                    result.setText(e.toString());
                    Toast.makeText(Dashboard.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }

            private float calBMI(Float weight, Float height) {
                return (weight/((height/100)*(height/100)));

            }

        });

    }
    private float predict(float gender,
                                float height,
                                float weight,
                                float ap_hi,
                                float ap_lo,
                                float cholesterol,
                                float gluc,
                                float smoke,
                                float alco,
                                float active,
                                float BMI,
                                float age_years) {

        float[][] input = {{
                gender, height, weight, ap_hi, ap_lo, cholesterol, gluc, smoke, alco, active, BMI, age_years
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