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
_basic_0:
mov $20, %r14d
mov $21, %r15d
mov $2, %r13d
cmp $2, %r13d
sete %r13b
cmp $1, %r13b
je _if_body_1
jmp _else_body_2
_if_body_1:
mov %r14d, %r12d
jmp _basic_3
_else_body_2:
mov %r15d, %r12d
jmp _basic_3
jmp _basic_4
_basic_3:
_basic_4:
mov %r12d, %eax
cltq
ret
