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
mov 0(%rsp), %r11
mov $100, %r11
mov %r11, 0(%rsp)
mov -1(%rsp), %r11
mov $40, %r11
mov %r11, -1(%rsp)
mov -1(%rsp), %r10
mov 0(%rsp), %r11
add %r10, %r11
mov %r10, -1(%rsp)
mov %r11, 0(%rsp)
mov 0(%rsp), %r10
mov %r10, %rax
mov %r10, 0(%rsp)
ret
