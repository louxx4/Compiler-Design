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
_while_body_0:
mov $3, %r12d
_basic_1:
mov $1, %r12b
cmp $0, %r12b
je _else_body_2
jmp _while_body_0
_else_body_2:
mov $4, %r12d
jmp _basic_3
_basic_3:
mov %r12d, %eax
cltq
ret
