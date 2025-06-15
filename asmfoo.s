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
mov $0, %r12b
cmp $1, %r12b
je _if_body_1
jmp _else_body_2
_if_body_1:
mov $11, %r12d
jmp _basic_3
_else_body_2:
mov $67, %r12d
jmp _basic_3
_basic_3:
mov %r12d, %eax
cltq
ret
