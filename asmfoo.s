.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
sub $8, %rsp
mov $0, %rdx
mov $42, %rax
mov $4, %r12
idiv %r12
mov %rdx, %r12
shl $2, %r12
mov $0, %rdx
mov $42, %rax
mov $4, %r14
idiv %r14
mov %rdx, %r14
shl $2, %r14
mov $0, %r13
sub %r14, %r13
imul %r13, %r12
mov $0, %rdx
mov $42, %rax
mov $4, %r13
idiv %r13
mov %rdx, %r13
shl $2, %r13
mov $0, %rdx
mov $42, %rax
mov $4, %r14
idiv %r14
mov %rdx, %r14
shl $2, %r14
mov $0, %r15
sub %r14, %r15
imul %r15, %r13
mov -8(%rsp), %r11
mov $0, %r11
mov %r11, -8(%rsp)
mov -8(%rsp), %r11
sub %r13, %r11
mov %r11, -8(%rsp)
mov $0, %rdx
mov $42, %rax
mov $4, %r15
idiv %r15
mov %rdx, %r15
shl $2, %r15
mov $0, %rdx
mov $42, %rax
mov $4, %r13
idiv %r13
mov %rdx, %r13
shl $2, %r13
mov $0, %r14
sub %r13, %r14
imul %r14, %r15
mov -8(%rsp), %r11
add %r15, %r11
mov %r11, -8(%rsp)
mov $0, %rdx
mov $42, %rax
mov $4, %r15
idiv %r15
mov %rdx, %r15
shl $2, %r15
mov $0, %rdx
mov $42, %rax
mov $4, %r13
idiv %r13
mov %rdx, %r13
shl $2, %r13
mov $0, %r14
sub %r13, %r14
imul %r14, %r15
mov -8(%rsp), %r11
sub %r15, %r11
mov %r11, -8(%rsp)
mov $0, %rdx
mov $42, %rax
mov $4, %r15
idiv %r15
mov %rdx, %r15
shl $2, %r15
mov $0, %rdx
mov $42, %rax
mov $4, %r13
idiv %r13
mov %rdx, %r13
shl $2, %r13
mov $0, %r14
sub %r13, %r14
imul %r14, %r15
mov -8(%rsp), %r11
sub %r15, %r11
mov %r11, -8(%rsp)
mov -8(%rsp), %r10
add %r10, %r12
mov %r10, -8(%rsp)
add $15, %r12
sub $37, %r12
mov %r12, %rax
add $8, %rsp
ret
