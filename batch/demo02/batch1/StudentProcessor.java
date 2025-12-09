package com.example.demo.batch1;

import com.example.demo.dto.Student;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class StudentProcessor implements ItemProcessor<Student, Student> {

    @Override
    public Student process(Student student) {
        String idStr = student.getName().replace("Student", "");
        int id = Integer.parseInt(idStr);
        if (id % 2 == 0) {
            return null;
        }

        return student;
    }
}
